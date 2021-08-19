package com.xsn.explorer.migrations

import com.xsn.explorer.data.anorm.AnormPostgresDataHandler
import com.xsn.explorer.data.async.{
  BlockFutureDataHandler,
  TransactionFutureDataHandler
}
import com.xsn.explorer.executors.DatabaseExecutionContext
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.db.Database

import scala.concurrent.ExecutionContext

@com.github.ghik.silencer.silent
class MigrationRunner @Inject() (
    transactionsDataHandler: TransactionFutureDataHandler,
    blockDataHandler: BlockFutureDataHandler,
    db: MigrationRunner.DatabaseOperations
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def run() = {}

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
  ) extends AnormPostgresDataHandler {}
}
