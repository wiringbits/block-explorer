package controllers

import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.data.async.{BlockFutureDataHandler, TransactionFutureDataHandler}
import com.xsn.explorer.gcs.{GolombCodedSet, GolombEncoding}
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.anorm.AnormPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.BlockFilterPostgresDAO
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.values.{Blockhash, Height}
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import play.api.db.Database

import scala.concurrent.Future

class MaintenanceController @Inject()(
    blockDataHandler: BlockFutureDataHandler,
    transactionDataHandler: TransactionFutureDataHandler,
    db: DBMigration,
    components: MyJsonControllerComponents
) extends MyJsonController(components) {

  def run(query: String) = public { _ =>
    val startingHeight = query.toInt

    logger.info(s"Migrating...")

    val startingState = Future.successful(Good(())).toFutureOr
    val finalState = (startingHeight to 0 by -1).foldLeft(startingState) {
      case (result, height) =>
        for {
          _ <- result
          block <- blockDataHandler.getBy(Height(height)).toFutureOr
          transactions <- transactionDataHandler.getTransactionsWithIOBy(block.hash, Limit(100000), None).toFutureOr
          blockWithTransactions = block.withTransactions(transactions)
          filter = GolombEncoding.encode(blockWithTransactions)
          _ <- filter
            .map(f => db.updateBlockFilter(block.hash, f))
            .getOrElse(Future.failed(new RuntimeException(s"Failed to generate filter at height ${height}")))
            .toFutureOr
          _ = logProgress(startingHeight, height)
        } yield ()
    }

    finalState.toFuture
      .map {
        case Good(_) => Good("")
        case Bad(errors) => Good(errors.toString)
      }
      .recoverWith {
        case error: Exception => Future.successful(Good(error.getMessage))
      }
  }

  def logProgress(startingHeight: Int, migratedHeight: Int) = {
    val blocksToMigrate: Int = startingHeight + 1
    val migratedBlocks: Int = startingHeight - migratedHeight + 1
    val percentage: Int = 100 * migratedBlocks / blocksToMigrate
    val previousPercentage: Int = 100 * (migratedBlocks - 1) / blocksToMigrate

    if (percentage != 0 && percentage != previousPercentage) {
      logger.info(s"migrated block at height ${migratedHeight}, ${percentage}% done")
    }
  }
}

class DBMigration @Inject()(override val database: Database, blockFilterPostgresDAO: BlockFilterPostgresDAO)(
    implicit dbEC: DatabaseExecutionContext
) extends AnormPostgresDataHandler {

  def updateBlockFilter(blockhash: Blockhash, newFilter: GolombCodedSet) = {
    Future {
      withTransaction { implicit conn =>
        val result = for {
          _ <- blockFilterPostgresDAO.delete(blockhash)
          _ <- Some(blockFilterPostgresDAO.insert(blockhash, newFilter))
        } yield ()

        result.map(Good(_)).getOrElse(throw new RuntimeException(s"Unable to recreate filter for ${blockhash}"))
      }
    }
  }
}
