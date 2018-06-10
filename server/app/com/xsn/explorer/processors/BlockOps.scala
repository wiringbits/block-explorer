package com.xsn.explorer.processors

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.DatabaseSeeder
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.errors.PostgresForeignKeyViolationError
import com.xsn.explorer.models.Transaction
import com.xsn.explorer.models.rpc.Block
import org.scalactic.{Bad, Good, One}

import scala.concurrent.{ExecutionContext, Future}

/**
 * BlockOps is contains useful operations used for seeding the database, while these methods might be adequate to be on
 * the [[com.xsn.explorer.services.BlockService]] class, they were added here to avoid growing that service,
 * in any case, these methods are used by the seeding process.
 */
class BlockOps @Inject() (
    databaseSeeder: DatabaseFutureSeeder,
    blockDataHandler: BlockFutureDataHandler)(
    implicit val ec: ExecutionContext) {

  import BlockOps._

  /**
   * Creates a new block and in case of a conflict due to repeated height, the old block is replaced.
   */
  def createBlock(block: Block, transactions: List[Transaction]): FutureApplicationResult[Result] = {
    val command = DatabaseSeeder.CreateBlockCommand(block, transactions)
    databaseSeeder
        .newBlock(command)
        .flatMap {
          case Good(_) => Future.successful(Good(Result.BlockCreated))
          case Bad(One(PostgresForeignKeyViolationError("height", _))) => onRepeatedBlockHeight(block, transactions)
          case Bad(errors) => Future.successful(Bad(errors))
        }
  }

  private def onRepeatedBlockHeight(newBlock: Block, newTransactions: List[Transaction]): FutureApplicationResult[Result] = {
    val result = for {
      orphanBlock <- blockDataHandler.getBy(newBlock.height).toFutureOr

      replaceCommand = DatabaseSeeder.ReplaceBlockCommand(
        orphanBlock = orphanBlock,
        newBlock = newBlock,
        newTransactions = newTransactions)

      _ <- databaseSeeder.replaceBlock(replaceCommand).toFutureOr
    } yield Result.BlockReplacedByHeight

    result.toFuture
  }
}

object BlockOps {

  sealed trait Result
  object Result {
    case object BlockCreated extends Result
    case object BlockReplacedByHeight extends Result
  }
}