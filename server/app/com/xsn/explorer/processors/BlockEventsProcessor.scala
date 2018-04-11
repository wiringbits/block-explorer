package com.xsn.explorer.processors

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.data.BlockBlockingDataHandler
import com.xsn.explorer.data.anorm.DatabasePostgresSeeder
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.Transaction
import com.xsn.explorer.services.XSNService
import com.xsn.explorer.util.Extensions.FutureApplicationResultExt
import org.scalactic.Good
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Process events related to blocks coming from the RPC server.
 */
class BlockEventsProcessor @Inject() (
    xsnService: XSNService,
    databasePostgresSeeder: DatabasePostgresSeeder,
    blockBlockingDataHandler: BlockBlockingDataHandler) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * There is a new latest block in the blockchain, we need to sync our database.
   *
   * The following scenarios are handled for the new latest block:
   *
   * 1. It is new on our database, we just append it.
   *    - current blocks = A -> B, new latest block = C, new blocks = A -> B -> C
   *    - current blocks = empty, new latest block = A, new blocks = A
   *
   * 2. It is an existing block, hence, the previous from the latest one in our database .
   *    - current blocks = A -> B -> C, new latest block = B, new blocks = A -> B
   *
   * @param blockhash the new latest block
   */
  def newLatestBlock(blockhash: Blockhash): FutureApplicationResult[Unit] = {
    val result = for {
      block <- xsnService.getBlock(blockhash).toFutureOr
      rpcTransactions <- block.transactions.map(xsnService.getTransaction).toFutureOr
      transactions = rpcTransactions.map(Transaction.fromRPC)
      r <- newLatestBlock(block, transactions).toFutureOr
    } yield r

    result.toFuture
  }

  private def newLatestBlock(newBlock: Block, newTransactions: List[Transaction]): FutureApplicationResult[Unit] = {
    def onRechain(orphanBlock: Block): FutureApplicationResult[Unit] = {
      val result = for {
        orphanRPCTransactions <- orphanBlock.transactions.map(xsnService.getTransaction).toFutureOr
        orphanTransactions = orphanRPCTransactions.map(Transaction.fromRPC)
      } yield {
        val command = DatabasePostgresSeeder.ReplaceBlockCommand(
          orphanBlock = orphanBlock,
          orphanTransactions = orphanTransactions,
          newBlock = newBlock,
          newTransactions = newTransactions)

        scala.concurrent.blocking {
          databasePostgresSeeder.replaceLatestBlock(command)
        }
      }

      result
          .mapWithError(identity)
          .toFuture
    }

    def onFirstBlock: FutureApplicationResult[Unit] = {
      logger.info(s"first block = ${newBlock.hash.string}")

      val command = DatabasePostgresSeeder.CreateBlockCommand(newBlock, newTransactions)
      def unsafe: ApplicationResult[Unit] = scala.concurrent.blocking {
        databasePostgresSeeder.firstBlock(command)
      }

      Future(unsafe)
    }

    def onNewBlock(latestBlock: Block): FutureApplicationResult[Unit] = {
      logger.info(s"existing latest block = ${latestBlock.hash.string} -> new latest block = ${newBlock.hash.string}")

      val command = DatabasePostgresSeeder.CreateBlockCommand(newBlock, newTransactions)
      def unsafe = scala.concurrent.blocking {
        databasePostgresSeeder.newLatestBlock(command)
      }

      Future(unsafe)
    }

    val latestBlockResult = scala.concurrent.blocking {
      blockBlockingDataHandler.getLatestBlock()
    }

    latestBlockResult
        .map { latestBlock =>
          if (newBlock.hash == latestBlock.hash) {
            // duplicated msg
            logger.info(s"ignoring duplicated latest block = ${newBlock.hash.string}")
            Future.successful(Good(()))
          } else if (newBlock.previousBlockhash.contains(latestBlock.hash)) {
            // latest block -> new block
            onNewBlock(latestBlock)
          } else {
            logger.info(s"rechain, orphan block = ${latestBlock.hash.string}, new latest block = ${newBlock.hash.string}")

            onRechain(latestBlock)
          }
        }
        .getOrElse(onFirstBlock)
  }
}
