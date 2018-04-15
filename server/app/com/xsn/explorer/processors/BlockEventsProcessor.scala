package com.xsn.explorer.processors

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.DatabaseSeeder
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Transaction}
import com.xsn.explorer.services.{TransactionService, XSNService}
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
    transactionService: TransactionService,
    databaseSeeder: DatabaseFutureSeeder,
    blockDataHandler: BlockFutureDataHandler) {

  import BlockEventsProcessor._

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
  def newLatestBlock(blockhash: Blockhash): FutureApplicationResult[Result] = {
    val result = for {
      block <- xsnService.getBlock(blockhash).toFutureOr
      transactions <- block.transactions.map(transactionService.getTransaction).toFutureOr
      r <- newLatestBlock(block, transactions).toFutureOr
    } yield r

    result.toFuture
  }

  private def newLatestBlock(newBlock: Block, newTransactions: List[Transaction]): FutureApplicationResult[Result] = {
    def onRechain(orphanBlock: Block): FutureApplicationResult[Result] = {
      val result = for {
        orphanTransactions <- orphanBlock.transactions.map(transactionService.getTransaction).toFutureOr

        command = DatabaseSeeder.ReplaceBlockCommand(
          orphanBlock = orphanBlock,
          orphanTransactions = orphanTransactions,
          newBlock = newBlock,
          newTransactions = newTransactions)
        _ <- databaseSeeder.replaceLatestBlock(command).toFutureOr
      } yield RechainDone(orphanBlock, newBlock)

      result.toFuture
    }

    def onFirstBlock: FutureApplicationResult[Result] = {
      logger.info(s"first block = ${newBlock.hash.string}")

      val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
      databaseSeeder.firstBlock(command)
          .toFutureOr
          .map(_ => FirstBlockCreated(newBlock))
          .toFuture
    }

    def onNewBlock(latestBlock: Block): FutureApplicationResult[Result] = {
      logger.info(s"existing latest block = ${latestBlock.hash.string} -> new latest block = ${newBlock.hash.string}")

      val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
      databaseSeeder
          .newLatestBlock(command)
          .toFutureOr
          .map(_ => NewBlockAppended(newBlock))
          .toFuture
    }

    def onMissingBlock(): FutureApplicationResult[Result] = {
      blockDataHandler
          .getBy(newBlock.hash)
          .flatMap {
            case Good(_) =>
              logger.info(s"The block ${newBlock.hash.string} is not missing but duplicated, ignoring")
              Future.successful { Good(ExistingBlockIgnored(newBlock)) }

            case Bad(One(BlockNotFoundError)) =>
              val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
              databaseSeeder
                  .insertPendingBlock(command)
                  .toFutureOr
                  .map(_ => MissingBlockProcessed(newBlock))
                  .toFuture

            case Bad(errors) =>
              Future.successful(Bad(errors))
          }
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

      r <- latestBlockMaybe
            .map { latestBlock =>
              if (newBlock.hash == latestBlock.hash) {
                // duplicated msg
                logger.info(s"ignoring duplicated latest block = ${newBlock.hash.string}")
                Future.successful(Good(ExistingBlockIgnored(newBlock)))
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
    } yield r

    result.toFuture
  }
}

object BlockEventsProcessor {

  sealed trait Result
  case class FirstBlockCreated(block: Block) extends Result
  case class MissingBlockProcessed(block: Block) extends Result
  case class ExistingBlockIgnored(block: Block) extends Result
  case class NewBlockAppended(block: Block) extends Result
  case class RechainDone(orphanBlock: Block, newBlock: Block) extends Result
}