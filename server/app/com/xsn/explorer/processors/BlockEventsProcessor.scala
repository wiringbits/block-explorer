package com.xsn.explorer.processors

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.data.BlockBlockingDataHandler
import com.xsn.explorer.data.anorm.DatabasePostgresSeeder
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.services.XSNService
import org.scalactic.Good
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

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
    xsnService
        .getBlock(blockhash)
        .toFutureOr
        .mapWithError { block =>
          scala.concurrent.blocking {
            newLatestBlock(block)
          }
        }
        .toFuture
  }

  private def newLatestBlock(newBlock: Block): ApplicationResult[Unit] = {
    val latestBlockResult = blockBlockingDataHandler.getLatestBlock()

    latestBlockResult
        .map { latestBlock =>
          if (newBlock.previousBlockhash.contains(latestBlock.hash)) {
            // latest block -> new block
            logger.info(s"existing latest block = ${latestBlock.hash.string} -> new latest block = ${newBlock.hash.string}")
            databasePostgresSeeder.newLatestBlock(newBlock)
          } else if (newBlock.hash == latestBlock.hash) {
            // duplicated msg
            logger.info(s"ignoring duplicated latest block = ${newBlock.hash.string}")
            Good(())
          } else {
            logger.info(s"orphan block = ${latestBlock.hash.string}, new latest block = ${newBlock.hash.string}")
            databasePostgresSeeder.replaceLatestBlock(newBlock, latestBlock.hash)
          }
        }
        .getOrElse {
          logger.info(s"first block = ${newBlock.hash.string}")
          databasePostgresSeeder.firstBlock(newBlock)
        }
  }
}
