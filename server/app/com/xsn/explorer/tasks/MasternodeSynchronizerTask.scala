package com.xsn.explorer.tasks

import akka.actor.{ActorSystem}
import com.xsn.explorer.config.MasternodeSynchronizerConfig
import com.xsn.explorer.services.{XSNService}
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory
import com.xsn.explorer.services.synchronizer.MasternodeSynchronizerActor

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MasternodeSynchronizerTask @Inject() (
    config: MasternodeSynchronizerConfig,
    xsnService: XSNService,
    actorSystem: ActorSystem,
    masternodeSynchronizerActor: MasternodeSynchronizerActor.Ref
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  @com.github.ghik.silencer.silent
  def start() = {

    if (config.enabled) {
      logger.info("Starting masternode synchronizer task")

      actorSystem.scheduler.schedule(config.initialDelay, config.interval) {
        xsnService.getMasternodes().onComplete {
          case Success(Good(masternodes)) =>
            logger.info(s"Masternode information synced ${masternodes.length}")
            masternodeSynchronizerActor.ref ! MasternodeSynchronizerActor
              .UpdateMasternodes(masternodes)
          case Success(Bad(error)) =>
            logger.warn(
              s"Masternode information syncronization failed due to $error"
            )
          case Failure(exception) =>
            logger.error(
              s"Masternode information syncronization failed due to ${exception.getMessage}",
              exception
            )
        }
      }
    } else {
      logger.info("Disabled masternode synchronizer task")
    }
  }
}
