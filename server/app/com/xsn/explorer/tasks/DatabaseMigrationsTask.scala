package com.xsn.explorer.tasks

import akka.actor.ActorSystem
import com.xsn.explorer.migrations.MigrationRunner
import javax.inject.Inject
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class DatabaseMigrationsTask @Inject()(
    actorSystem: ActorSystem,
    migration: MigrationRunner
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  def start() = {
    logger.info("Starting database migrations task")
    actorSystem.scheduler.scheduleOnce(10 seconds) {
      migration.run
    }
  }
}
