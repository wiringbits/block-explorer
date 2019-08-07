package controllers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.anorm.AnormPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.BlockFilterPostgresDAO
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.values.{Blockhash, Height}
import com.xsn.explorer.services.{TransactionCollectorService, XSNService}
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import play.api.db.Database

import scala.concurrent.Future
import scala.util.{Failure, Success}

class MaintenanceController @Inject()(
    xsnService: XSNService,
    transactionCollectorService: TransactionCollectorService,
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
          blockhash <- xsnService.getBlockhash(Height(height)).toFutureOr
          block <- xsnService.getFullBlock(blockhash).toFutureOr
          data <- transactionCollectorService.collect(block).toFutureOr
          (_, _, filterFactory) = data
          _ <- db.updateBlockFilter(block.hash, filterFactory()).toFutureOr
          _ = logProgress(startingHeight, height)
        } yield ()
    }

    finalState.toFuture.onComplete {
      case Failure(ex) => logger.error("Migration failed", ex)
      case Success(Bad(e)) => logger.error(s"Migration failed, errors = $e")
      case Success(Good(_)) => logger.info("Migration completed")
    }

    Future.successful(Good("Ok"))
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

  def updateBlockFilter(blockhash: Blockhash, newFilter: GolombCodedSet): FutureApplicationResult[Unit] = {
    Future {
      withConnection { implicit conn =>
        blockFilterPostgresDAO.upsert(blockhash, newFilter)
        Good(())
      }
    }
  }
}
