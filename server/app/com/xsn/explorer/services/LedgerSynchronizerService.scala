package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OptionOps}
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.data.async.{BlockFutureDataHandler, LedgerFutureDataHandler}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.loggers.LedgerSynchronizerServiceLogger
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.transformers._
import com.xsn.explorer.models.values._
import com.xsn.explorer.util.Extensions.FutureOrExt
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LedgerSynchronizerService @Inject()(
    explorerConfig: ExplorerConfig,
    xsnService: XSNService,
    blockService: BlockService,
    transactionCollectorService: TransactionCollectorService,
    ledgerDataHandler: LedgerFutureDataHandler,
    blockDataHandler: BlockFutureDataHandler
)(implicit ec: ExecutionContext) {

  import LedgerSynchronizerService._

  /**
   * Synchronize the given block with our ledger database.
   *
   * The synchronization involves a very complex logic in order to handle
   * several corner cases, be sure to not call this method concurrently
   * because the behavior is undefined.
   */
  def synchronize(blockhash: Blockhash): FutureApplicationResult[Unit] = {
    val logger = LedgerSynchronizerServiceLogger(LoggerFactory.getLogger(this.getClass))
    val result = for {
      data <- getRPCBlock(blockhash).toFutureOr
      nextLogger <- synchronize(data, logger).toFutureOr
      _ <- Future.successful(Good(nextLogger.syncCompleted(data))).toFutureOr
    } yield ()

    result.toFuture
  }

  private def synchronize(
      block: rpc.Block,
      logger: LedgerSynchronizerServiceLogger
  ): FutureApplicationResult[LedgerSynchronizerServiceLogger] = {
    val result = for {
      latestBlockMaybe <- blockDataHandler
        .getLatestBlock()
        .toFutureOr
        .map(Option.apply)
        .recoverFrom(BlockNotFoundError)(None)

      lastLogger <- latestBlockMaybe
        .map { latestBlock =>
          onLatestBlock(latestBlock, block, logger)
        }
        .getOrElse { onEmptyLedger(block, logger) }
        .toFutureOr
    } yield lastLogger

    result.toFuture
  }

  /**
   * 1. current ledger is empty:
   * 1.1. the given block is the genensis block, it is added.
   * 1.2. the given block is not the genesis block, sync everything until the given block.
   */
  private def onEmptyLedger(
      block: rpc.Block,
      logger: LedgerSynchronizerServiceLogger
  ): FutureApplicationResult[LedgerSynchronizerServiceLogger] = {
    if (block.height.int == 0) {
      val result = for {
        _ <- appendBlock(block, logger).toFutureOr
        nextLogger <- Future.successful(Good(logger.blockSynched(None, block))).toFutureOr
      } yield nextLogger

      result.toFuture
    } else {
      val result = for {
        nextLogger <- sync(0 until block.height.int, logger).toFutureOr
        lastLogger <- synchronize(block, nextLogger).toFutureOr
      } yield lastLogger

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
  private def onLatestBlock(
      ledgerBlock: Block,
      newBlock: rpc.Block,
      logger: LedgerSynchronizerServiceLogger
  ): FutureApplicationResult[LedgerSynchronizerServiceLogger] = {
    if (ledgerBlock.height.int + 1 == newBlock.height.int &&
      newBlock.previousBlockhash.contains(ledgerBlock.hash)) {

      val result = for {
        _ <- appendBlock(newBlock, logger).toFutureOr
        nextLogger <- Future.successful(Good(logger.blockSynched(Some(ledgerBlock), newBlock))).toFutureOr
      } yield nextLogger

      result.toFuture
    } else if (ledgerBlock.height.int + 1 == newBlock.height.int) {
      val result = for {
        blockhash <- newBlock.previousBlockhash.toFutureOr(BlockNotFoundError)
        previousBlock <- getRPCBlock(blockhash).toFutureOr
        logger2 <- Future.successful(Good(logger.reorganize)).toFutureOr
        logger3 <- synchronize(previousBlock, logger2).toFutureOr
        lastLogger <- synchronize(newBlock, logger3).toFutureOr
      } yield lastLogger

      result.toFuture
    } else if (newBlock.height.int > ledgerBlock.height.int) {
      val result = for {
        nextLogger <- sync(ledgerBlock.height.int + 1 until newBlock.height.int, logger).toFutureOr
        lastLogger <- synchronize(newBlock, nextLogger).toFutureOr
      } yield lastLogger

      result.toFuture
    } else {
      val result = for {
        expectedBlockMaybe <- blockDataHandler
          .getBy(newBlock.hash)
          .toFutureOr
          .map(Option.apply)
          .recoverFrom(BlockNotFoundError)(None)

        lastLogger <- expectedBlockMaybe
          .map { _ =>
            Future.successful(Good(logger))
          }
          .getOrElse {
            val x = for {
              _ <- trimTo(newBlock.height).toFutureOr
              nextLogger <- Future.successful(Good(logger.trimed)).toFutureOr
              lastLogger <- synchronize(newBlock, nextLogger).toFutureOr
            } yield lastLogger
            x.toFuture
          }
          .toFutureOr
      } yield lastLogger

      result.toFuture
    }
  }

  private def appendBlock(
      newBlock: rpc.Block,
      logger: LedgerSynchronizerServiceLogger
  ): FutureApplicationResult[Unit] = {
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
  private def sync(
      range: Range,
      logger: LedgerSynchronizerServiceLogger
  ): FutureApplicationResult[LedgerSynchronizerServiceLogger] = {
    // TODO: check, it might be safer to use the nextBlockhash instead of the height
    range.foldLeft[FutureApplicationResult[LedgerSynchronizerServiceLogger]](Future.successful(Good(logger))) {
      case (previous, height) =>
        val result = for {
          currentLogger <- previous.toFutureOr
          blockhash <- xsnService.getBlockhash(Height(height)).toFutureOr
          block <- getRPCBlock(blockhash).toFutureOr
          newLogger <- synchronize(block, currentLogger).toFutureOr
        } yield newLogger

        result.toFuture
    }
  }

  private def getRPCBlock(blockhash: Blockhash): FutureApplicationResult[rpc.Block] = {
    val result = for {
      rpcBlock <- xsnService.getBlock(blockhash).toFutureOr
    } yield {
      if (explorerConfig.liteVersionConfig.enabled &&
        rpcBlock.height.int < explorerConfig.liteVersionConfig.syncTransactionsFromBlock) {

        // lite version, ignore transactions
        rpcBlock.copy(transactions = List.empty)
      } else {
        rpcBlock
      }
    }

    result.toFuture
  }

  private def ExcludedTransactions =
    List("e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468").flatMap(TransactionId.from)

  private def getBlockData(rpcBlock: rpc.Block): FutureApplicationResult[BlockData] = {
    val result = for {
      extractionMethod <- blockService.extractionMethod(rpcBlock).toFutureOr
      data <- transactionCollectorService.collect(rpcBlock.transactions, ExcludedTransactions).toFutureOr
      (transactions, contracts) = data
      validContracts <- getValidContracts(contracts).toFutureOr
      filteredTransactions = transactions.filter(_.blockhash == rpcBlock.hash)
    } yield {

      val block = toPersistedBlock(rpcBlock, extractionMethod).withTransactions(filteredTransactions)
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
      x.foldLeft(initial) {
        case (acc, cur) =>
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
