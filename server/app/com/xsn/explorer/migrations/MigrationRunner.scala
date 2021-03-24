package com.xsn.explorer.migrations

import anorm.{SQL, SqlParser}
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.data.anorm.AnormPostgresDataHandler
import com.xsn.explorer.data.async.{BlockFutureDataHandler, TransactionFutureDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.TransactionWithValues
import com.xsn.explorer.models.values.Height
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory
import play.api.db.Database

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@com.github.ghik.silencer.silent
class MigrationRunner @Inject() (
    transactionsDataHandler: TransactionFutureDataHandler,
    blockDataHandler: BlockFutureDataHandler,
    db: MigrationRunner.DatabaseOperations
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def run() = {
    val targetBlock = blockDataHandler.getLatestBlock().toFutureOr

    targetBlock.map { targetBlock =>
      val startingBlock = db.getStartingBlock()
      val startingState = Future.successful(Good(())).toFutureOr

      logger.info(
        s"Migrating transactions sent/received amounts from block $startingBlock to block ${targetBlock.height}"
      )

      val rangeSize = 10000
      val blockRanges = (startingBlock until targetBlock.height.int by rangeSize).map { start =>
        val end = start + rangeSize - 1

        (start, end)
      }

      val finalState = blockRanges.foldLeft(startingState) { case (state, (start, end)) =>
        for {
          _ <- state
          _ <- db.updateBlocksTransactions(start, end).toFutureOr
          _ = logProgress(end, targetBlock.height.int)
        } yield ()
      }

      finalState.toFuture.onComplete {
        case Success(Good(_)) => logger.info("transactions successfully migrated")
        case Success(Bad(error)) => logger.info(s"transactions migration failed due to ${error.toString}")
        case Failure(error) => logger.info(s"transactions migration failed due to ${error.toString}")
      }
    }
  }

  private def logProgress(migratedHeight: Int, targetHeight: Int) = {
    val percentage: Int = 100 * migratedHeight / targetHeight
    val previousPercentage: Int = 100 * (migratedHeight - 1) / targetHeight

    if (percentage != 0 && percentage != previousPercentage) {
      logger.info(
        s"migrated block at height $migratedHeight, $percentage% done"
      )
    }
  }
}

object MigrationRunner {

  @com.github.ghik.silencer.silent
  class DatabaseOperations @Inject() (override val database: Database)(implicit
      dbEC: DatabaseExecutionContext
  ) extends AnormPostgresDataHandler {

    def updateBlocksTransactions(start: Int, end: Int) = Future {
      withConnection { implicit conn =>
        SQL(
          """
              |UPDATE transactions AS t 
              |SET sent = (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid),
              |    received = (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid)
              |FROM blocks AS b
              |WHERE t.blockhash = b.blockhash
              |  AND b.height >= {start}
              |  AND b.height <= {end}
      """.stripMargin
        ).on(
          'start -> start,
          'end -> end
        ).execute

        Good(())
      }
    }

    def getStartingBlock(): Int = {
      database.withConnection { implicit conn =>
        SQL(
          """
            |SELECT
            |  max(height)
            |FROM transactions
            |INNER JOIN blocks USING(blockhash)
            |WHERE sent IS NOT NULL
            |  AND height < 1590000 
            |""".stripMargin
        )
          .as(SqlParser.scalar[Int].singleOpt)
          .getOrElse(0)
      }
    }
  }
}
