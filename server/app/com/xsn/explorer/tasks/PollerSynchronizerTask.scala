package com.xsn.explorer.tasks

import akka.actor.ActorSystem
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.config.LedgerSynchronizerConfig
import com.xsn.explorer.services
import com.xsn.explorer.services.XSNService
import javax.inject.Inject
import org.scalactic.Bad
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class PollerSynchronizerTask @Inject()(
    config: LedgerSynchronizerConfig,
    actorSystem: ActorSystem,
    xsnService: XSNService,
    ledgerSynchronizerService: services.LedgerSynchronizerService,
    newSynchronizer: services.synchronizer.LedgerSynchronizerService
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  if (config.enabled) {
    start()
  } else {
    logger.info("The polled synchronizer is not enabled")
  }

  def start() = {
    logger.info("Starting the poller synchronizer task")
    actorSystem.scheduler.scheduleOnce(config.initialDelay) {
      run()
    }
  }

  private def run(): Unit = {
    val result = for {
      block <- xsnService.getLatestBlock().toFutureOr
      _ <- if (config.useNewSynchronizer) {
        newSynchronizer.synchronize(block.hash).toFutureOr
      } else {
        ledgerSynchronizerService.synchronize(block.hash).toFutureOr
      }
    } yield ()

    result.toFuture
      .map {
        case Bad(errors) => logger.error(s"Failed to sync latest block, errors = $errors")
        case _ => ()
      }
      .recover {
        case NonFatal(ex) => logger.error("Failed to sync latest block", ex)
      }
      .foreach { _ =>
        actorSystem.scheduler.scheduleOnce(config.interval) { run() }
      }
  }
}
