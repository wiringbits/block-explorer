package com.xsn.explorer.tasks

import javax.inject.{Inject, Singleton}

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.data.DatabaseSeeder
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Height}
import com.xsn.explorer.services.{TransactionService, XSNService}
import com.xsn.explorer.util.Extensions.FutureApplicationResultExt
import org.scalactic.{Bad, Good, One, Or}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * As the dababse sync process is slow, it's useful to fill it using a fast server and then, release to the
 * production server, in this case might have a database updated until block N while the SQS messages started
 * at N + X, leaving X missing blocks, this task, syncs the database from block N + X to block N.
 */
@Singleton
class BackwardsSynchronizerTask @Inject() (
    xsnService: XSNService,
    transactionService: TransactionService,
    blockDataHandler: BlockFutureDataHandler,
    databaseSeeder: DatabaseFutureSeeder) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val lock = new Object

  /**
   * TODO: Until https://github.com/X9Developers/XSN/issues/32 is fixed, ignore the genesis block.
   */
  private val FirstBlockHeight = Height(1)

  private var running = false

  /**
   *
   * @param block a block that is stored in our database
   */
  def sync(block: Block): Unit = {
    if (running) {
      ()
    } else {
      lock.synchronized {
        if (running) {
          ()
        } else {
          running = true

          tryToSync(block).onComplete { _ =>
            running = false
          }
        }
      }
    }
  }

  private def tryToSync(block: Block) = {
    val futureOr = for {
      previous <- Or.from(block.previousBlockhash, One(BlockNotFoundError)).toFutureOr
      _ <- {
        if (block.height == FirstBlockHeight) {
          // no sync required
          Future.successful(Good(()))
        } else {
          // sync
          logger.info(s"Sync might be required from block ${block.height.int}")
          val r = doSync(previous)

          r.foreach {
            case Good(_) =>
              logger.info("Sync completed")
            case _ => ()
          }

          r
        }
      }.toFutureOr
    } yield ()

    val result = futureOr.toFuture

    result.foreach {
      case Bad(errors) =>
        logger.error(s"Failed to sync blocks, errors = $errors")

      case _ => ()
    }

    result.recover {
      case NonFatal(ex) =>
        logger.error(s"Failed to sync blocks", ex)
    }

    result
  }

  private def checkAndSync(blockhash: Blockhash): FutureApplicationResult[Unit] = {
    blockDataHandler.getBy(blockhash).flatMap {
      case Bad(One(BlockNotFoundError)) => doSync(blockhash)
      case Bad(errors) => Future.successful(Bad(errors))
      case Good(_) =>
        logger.debug("No more blocks to sync")
        Future.successful(Good(()))
    }
  }

  private def doSync(blockhash: Blockhash): FutureApplicationResult[Unit] = {
    val result = for {
      block <- xsnService.getBlock(blockhash).toFutureOr
      transactions <- block.transactions.map(transactionService.getTransaction).toFutureOr

      command = DatabaseSeeder.CreateBlockCommand(block, transactions)
      _ <- databaseSeeder.insertPendingBlock(command).toFutureOr
      _ = logger.debug(s"Block ${block.height.int} saved")

      _ <- block
          .previousBlockhash
          .filter(_ => block.height.int > FirstBlockHeight.int)
          .map { previous =>
            checkAndSync(previous)
          }
          .getOrElse {
            logger.debug(s"No more blocks to sync")
            Future.successful(Good(()))
          }
          .toFutureOr
    } yield ()

    result.toFuture
  }
}
