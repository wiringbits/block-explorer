package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OptionOps}
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.data.async.{BlockFutureDataHandler, LedgerFutureDataHandler}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.transformers._
import com.xsn.explorer.models.values._
import com.xsn.explorer.util.Extensions.FutureOrExt
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LedgerSynchronizerService @Inject() (
    xsnService: XSNService,
    blockService: BlockService,
    transactionCollectorService: TransactionCollectorService,
    ledgerDataHandler: LedgerFutureDataHandler,
    blockDataHandler: BlockFutureDataHandler)(
    implicit ec: ExecutionContext) {

  import LedgerSynchronizerService._

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Synchronize the given block with our ledger database.
   *
   * The synchronization involves a very complex logic in order to handle
   * several corner cases, be sure to not call this method concurrently
   * because the behavior is undefined.
   */
  def synchronize(blockhash: Blockhash): FutureApplicationResult[Unit] = {
    val result = for {
      data <- xsnService.getBlock(blockhash).toFutureOr
      _ <- synchronize(data).toFutureOr
    } yield ()

    result.toFuture
  }

  private def synchronize(block: rpc.Block): FutureApplicationResult[Unit] = {
    logger.info(s"Synchronize block ${block.height}, hash = ${block.hash}")

    val result = for {
      latestBlockMaybe <- blockDataHandler
          .getLatestBlock()
          .toFutureOr
          .map(Option.apply)
          .recoverFrom(BlockNotFoundError)(None)

      _ <- latestBlockMaybe
          .map { latestBlock => onLatestBlock(latestBlock, block) }
          .getOrElse { onEmptyLedger(block) }
          .toFutureOr
    } yield ()

    result.toFuture
  }

  /**
   * 1. current ledger is empty:
   * 1.1. the given block is the genensis block, it is added.
   * 1.2. the given block is not the genesis block, sync everything until the given block.
   */
  private def onEmptyLedger(block: rpc.Block): FutureApplicationResult[Unit] = {
    if (block.height.int == 0) {
      logger.info(s"Synchronize genesis block on empty ledger, hash = ${block.hash}")
      appendBlock(block)
    } else {
      logger.info(s"Synchronize block ${block.height} on empty ledger, hash = ${block.hash}")
      val result = for {
        _ <- sync(0 until block.height.int).toFutureOr
        _ <- synchronize(block).toFutureOr
      } yield ()

      result.toFuture
    }
  }

  /**
   * 2. current ledger has blocks until N, given block height H:
   * 2.1. if N+1 == H and its previous blockhash is N, it is added.
   * 2.2. if N+1 == H and its previous blockhash isn't N, pick the expected block N from H and apply the whole process with it, then, apply H.
   * 2.3. if H > N+1, sync everything until H.
   * 2.4. if H <= N, if the hash already exists, it is ignored.
   * 2.5. if H <= N, if the hash doesn't exists, remove blocks from N to H (included), then, add the new H.
   */
  private def onLatestBlock(ledgerBlock: Block, newBlock: rpc.Block): FutureApplicationResult[Unit] = {
    if (ledgerBlock.height.int + 1 == newBlock.height.int &&
        newBlock.previousBlockhash.contains(ledgerBlock.hash)) {

      logger.info(s"Appending block ${newBlock.height}, hash = ${newBlock.hash}")
      appendBlock(newBlock)
    } else if (ledgerBlock.height.int + 1 == newBlock.height.int) {
      logger.info(s"Reorganization to push block ${newBlock.height}, hash = ${newBlock.hash}")
      val result = for {
        blockhash <- newBlock.previousBlockhash.toFutureOr(BlockNotFoundError)
        previousBlock <- xsnService.getBlock(blockhash).toFutureOr
        _ <- synchronize(previousBlock).toFutureOr
        _ <- synchronize(newBlock).toFutureOr
      } yield ()

      result.toFuture
    } else if (newBlock.height.int > ledgerBlock.height.int) {
      logger.info(s"Filling holes to push block ${newBlock.height}, hash = ${newBlock.hash}")
      val result = for {
        _ <- sync(ledgerBlock.height.int + 1 until newBlock.height.int).toFutureOr
        _ <- synchronize(newBlock).toFutureOr
      } yield ()

      result.toFuture
    } else {
      val result = for {
        expectedBlockMaybe <- blockDataHandler
            .getBy(newBlock.hash)
            .toFutureOr
            .map(Option.apply)
            .recoverFrom(BlockNotFoundError)(None)

        _ = logger.info(s"Checking possible existing block ${newBlock.height}, hash = ${newBlock.hash}, exists = ${expectedBlockMaybe.isDefined}")
        _ <- expectedBlockMaybe
            .map { _ => Future.successful(Good(())) }
            .getOrElse {
              val x = for {
                _ <- trimTo(newBlock.height).toFutureOr
                _ <- synchronize(newBlock).toFutureOr
              } yield ()
              x.toFuture
            }
            .toFutureOr
      } yield ()

      result.toFuture
    }
  }

  private def appendBlock(newBlock: rpc.Block): FutureApplicationResult[Unit] = {
    val result = for {
      data <- getBlockData(newBlock).toFutureOr
      (blockWithTransactions, tposContracts) = data
      _ <- ledgerDataHandler.push(blockWithTransactions, tposContracts).toFutureOr
    } yield ()

    result.toFuture
  }

  /**
   * Sync the given range to our ledger.
   */
  private def sync(range: Range): FutureApplicationResult[Unit] = {
    logger.info(s"Syncing block range = $range")

    // TODO: check, it might be safer to use the nextBlockhash instead of the height
    range.foldLeft[FutureApplicationResult[Unit]](Future.successful(Good(()))) { case (previous, height) =>
      val result = for {
        _ <- previous.toFutureOr
        blockhash <- xsnService.getBlockhash(Height(height)).toFutureOr
        block <- xsnService.getBlock(blockhash).toFutureOr
        _ <- synchronize(block).toFutureOr
      } yield ()

      result.toFuture
    }
  }

  private def getBlockData(rpcBlock: rpc.Block): FutureApplicationResult[BlockData] = {
    val result = for {
      extractionMethod <- blockService.extractionMethod(rpcBlock).toFutureOr
      data <- transactionCollectorService.collect(rpcBlock.transactions).toFutureOr
      (transactions, contracts) = data
      validContracts <- getValidContracts(contracts).toFutureOr
    } yield {
      val block = toPersistedBlock(rpcBlock, extractionMethod).withTransactions(transactions)
      (block, validContracts)
    }

    result.toFuture
  }

  private def getValidContracts(contracts: List[TPoSContract]): FutureApplicationResult[List[TPoSContract]] = {
    val listF = contracts
        .map { contract =>
          xsnService
              .isTPoSContract(contract.txid)
              .toFutureOr
              .map { valid =>
                if (valid) Some(contract)
                else None
              }
              .toFuture
        }

    val futureList = Future.sequence(listF)
    futureList.map { list =>
      val x = list.flatMap {
        case Good(a) => a.map(Good(_))
        case Bad(e) => Some(Bad(e))
      }

      val initial: ApplicationResult[List[TPoSContract]] = Good(List.empty)
      x.foldLeft(initial) { case (acc, cur) =>
        cur match {
          case Good(contract) => acc.map(contract :: _)
          case Bad(e) => acc.badMap(prev => prev ++ e)
        }
      }
    }
  }

  /**
   * Trim the ledger until the given block height, if the height is 4,
   * the last stored block will be 3.
   */
  private def trimTo(height: Height): FutureApplicationResult[Unit] = {
    val result = ledgerDataHandler
        .pop()
        .toFutureOr
        .flatMap { block =>
          logger.info(s"Trimmed block ${block.height} from the ledger")
          val result = if (block.height == height) {
            Future.successful(Good(()))
          } else {
            trimTo(height)
          }

          result.toFutureOr
        }

    result.toFuture
  }
}

object LedgerSynchronizerService {

  type BlockData = (Block.HasTransactions, List[TPoSContract])
}
