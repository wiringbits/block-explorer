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
      logger.info(s"Migrating transactions sent/received amounts from block 0 to block ${targetBlock.height}")

      val startingBlock = db.getStartingBlock()
      val startingState = Future.successful(Good(())).toFutureOr
      val finalState = (startingBlock to targetBlock.height.int).foldLeft(startingState) { case (state, height) =>
        for {
          _ <- state
          block <- blockDataHandler.getBlock(Height(height)).toFutureOr
          transactions <- transactionsDataHandler.getByBlockhash(block.hash, Limit(Integer.MAX_VALUE), None).toFutureOr
          _ <- db.updateTransactions(transactions).toFutureOr
          _ = logProgress(height, targetBlock.height.int)
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

    def updateTransactions(transactions: List[TransactionWithValues]) = Future {
      withConnection { implicit conn =>
        transactions.foreach { transaction =>
          SQL(
            """
              |UPDATE transactions
              |SET sent = {sent}::AMOUNT_TYPE, received = {received}::AMOUNT_TYPE
              |WHERE txid = {txid}
      """.stripMargin
          ).on(
            'txid -> transaction.id.toBytesBE.toArray,
            'sent -> transaction.sent,
            'received -> transaction.received
          ).execute
        }

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
