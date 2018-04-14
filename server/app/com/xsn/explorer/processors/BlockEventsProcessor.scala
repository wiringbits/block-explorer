package com.xsn.explorer.processors

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.DatabaseSeeder
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Transaction}
import com.xsn.explorer.services.XSNService
import com.xsn.explorer.util.Extensions.FutureApplicationResultExt
import org.scalactic.{Bad, Good, One}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Process events related to blocks coming from the RPC server.
 */
class BlockEventsProcessor @Inject() (
    xsnService: XSNService,
    databaseSeeder: DatabaseFutureSeeder,
    blockDataHandler: BlockFutureDataHandler) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * There is a new latest block in the blockchain, we need to sync our database.
   *
   * The following scenarios are handled for the new latest block:
   *
   * 1. It is new on our database, we just append it (possibly first block).
   *    - current blocks = A -> B, new latest block = C, new blocks = A -> B -> C
   *    - current blocks = empty, new latest block = A, new blocks = A
   *
   * 2. It is the previous block from our latest block (rechain).
   *    - current blocks = A -> B -> C, new latest block = B, new blocks = A -> B
   *
   * 3. None of previous cases, it is a block that might be missing in our chain.
   *
   * @param blockhash the new latest block
   */
  def newLatestBlock(blockhash: Blockhash): FutureApplicationResult[Block] = {
    val result = for {
      block <- xsnService.getBlock(blockhash).toFutureOr
      rpcTransactions <- block.transactions.map(xsnService.getTransaction).toFutureOr
      transactions = rpcTransactions.map(Transaction.fromRPC)
      r <- newLatestBlock(block, transactions).toFutureOr
    } yield block

    result.toFuture
  }

  private def newLatestBlock(newBlock: Block, newTransactions: List[Transaction]): FutureApplicationResult[Unit] = {
    def onRechain(orphanBlock: Block): FutureApplicationResult[Unit] = {
      val result = for {
        orphanRPCTransactions <- orphanBlock.transactions.map(xsnService.getTransaction).toFutureOr
        orphanTransactions = orphanRPCTransactions.map(Transaction.fromRPC)

        command = DatabaseSeeder.ReplaceBlockCommand(
          orphanBlock = orphanBlock,
          orphanTransactions = orphanTransactions,
          newBlock = newBlock,
          newTransactions = newTransactions)
        _ <- databaseSeeder.replaceLatestBlock(command).toFutureOr
      } yield ()

      result.toFuture
    }

    def onFirstBlock: FutureApplicationResult[Unit] = {
      logger.info(s"first block = ${newBlock.hash.string}")

      val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
      databaseSeeder.firstBlock(command)
    }

    def onNewBlock(latestBlock: Block): FutureApplicationResult[Unit] = {
      logger.info(s"existing latest block = ${latestBlock.hash.string} -> new latest block = ${newBlock.hash.string}")

      val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
      databaseSeeder.newLatestBlock(command)
    }

    def onMissingBlock(): FutureApplicationResult[Unit] = {
      val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
      databaseSeeder.insertPendingBlock(command)
    }

    val result = for {
      latestBlockMaybe <- blockDataHandler
          .getLatestBlock()
          .map {
            case Good(block) => Good(Some(block))
            case Bad(One(BlockNotFoundError)) => Good(None)
            case Bad(errors) => Bad(errors)
          }
          .toFutureOr

      _ <- latestBlockMaybe
            .map { latestBlock =>
              if (newBlock.hash == latestBlock.hash) {
                // duplicated msg
                logger.info(s"ignoring duplicated latest block = ${newBlock.hash.string}")
                Future.successful(Good(()))
              } else if (newBlock.previousBlockhash.contains(latestBlock.hash)) {
                // latest block -> new block
                onNewBlock(latestBlock)
              } else if (latestBlock.previousBlockhash.contains(newBlock.hash)) {
                logger.info(s"rechain, orphan block = ${latestBlock.hash.string}, new latest block = ${newBlock.hash.string}")

                onRechain(latestBlock)
              } else {
                logger.info(s"Adding possible missing block = ${newBlock.hash.string}")
                onMissingBlock()
              }
            }
            .getOrElse(onFirstBlock)
            .toFutureOr
    } yield ()

    result.toFuture
  }
}
