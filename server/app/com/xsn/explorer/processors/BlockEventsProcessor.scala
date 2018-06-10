package com.xsn.explorer.processors

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureListOps, FutureOps}
import com.xsn.explorer.data.DatabaseSeeder
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.errors._
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Transaction}
import com.xsn.explorer.services.{TransactionService, XSNService}
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
    blockDataHandler: BlockFutureDataHandler,
    blockOps: BlockOps) {

  import BlockEventsProcessor._

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * There is a possible new block in the blockchain, we need to sync our database.
   *
   * The following scenarios are handled for the new block:
   *
   * 0. The block is already present, we ignore it.
   *
   * x. The block can not be fully retrieved from the rpc server, we ignore it.
   *
   * x. The first block in the database (not exactly the first block in the chain), we just add it.
   *    - Example: current blocks = empty, new block = A, new blocks = A
   *
   * x. A new block on our database having a link to our latest block in the database:
   *    - The new block is added.
   *    - The next block from our latest block is set to the next block.
   *    - Example: current blocks = A -> B, new block = C, new blocks = A -> B -> C
   *
   * 2. It is the previous block from our latest block (rechain).
   *    - current blocks = A -> B -> C, new latest block = B, new blocks = A -> B
   *
   * 3. None of previous cases, it is a block that might be missing in our chain.
   *
   * @param blockhash the blockhash to process
   */
  def processBlock(blockhash: Blockhash): FutureApplicationResult[Result] = {
    val result = for {
      block <- xsnService.getBlock(blockhash).toFutureOr
      transactions <- block.transactions.map(transactionService.getTransaction).toFutureOr
      r <- processBlock(block, transactions).toFutureOr
    } yield r

    result.toFuture.map {
      case Good(r) => Good(r)
      case Bad(One(BlockNotFoundError)) => Good(MissingBlockIgnored)
      case Bad(One(TransactionNotFoundError)) => Good(MissingBlockIgnored)
      case Bad(errors) => Bad(errors)
    }
  }

  private def processBlock(newBlock: Block, newTransactions: List[Transaction]): FutureApplicationResult[Result] = {
    def onRechain(orphanBlock: Block): FutureApplicationResult[Result] = {
      val command = DatabaseSeeder.ReplaceBlockCommand(
        orphanBlock = orphanBlock,
        newBlock = newBlock,
        newTransactions = newTransactions)

      val result = for {
        _ <- databaseSeeder.replaceBlock(command).toFutureOr
      } yield RechainDone(orphanBlock, newBlock)

      result.toFuture
    }

    def onFirstBlock: FutureApplicationResult[Result] = {
      logger.info(s"first block = ${newBlock.hash.string}")

      val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
      databaseSeeder.newBlock(command)
          .toFutureOr
          .map(_ => FirstBlockCreated(newBlock))
          .toFuture
    }

    def onNewBlock(latestBlock: Block): FutureApplicationResult[Result] = {
      logger.info(s"existing latest block = ${latestBlock.hash.string} -> new latest block = ${newBlock.hash.string}")

      val command = DatabaseSeeder.CreateBlockCommand(newBlock, newTransactions)
      databaseSeeder
          .newBlock(command)
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
              blockOps
                  .createBlock(newBlock, newTransactions)
                  .map {
                    case Good(BlockOps.Result.BlockCreated) => Good(MissingBlockProcessed(newBlock))
                    case Good(BlockOps.Result.BlockReplacedByHeight) => Good(ReplacedByBlockHeight)
                    case Bad(errors) => Bad(errors)
                  }

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
  case object MissingBlockIgnored extends Result
  case object ReplacedByBlockHeight extends Result
}