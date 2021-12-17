package com.xsn.explorer.tasks

import akka.actor.ActorSystem
import com.xsn.explorer.config.MerchantnodeSynchronizerConfig
import com.xsn.explorer.services.XSNService
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory
import com.xsn.explorer.services.synchronizer.MerchantnodeSynchronizerActor

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MerchantnodeSynchronizerTask @Inject() (
    config: MerchantnodeSynchronizerConfig,
    xsnService: XSNService,
    actorSystem: ActorSystem,
    merchantnodeSynchronizerActor: MerchantnodeSynchronizerActor.Ref
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  @com.github.ghik.silencer.silent
  def start() = {

    if (config.enabled) {
      logger.info("Starting merchantnode synchronizer task")

      actorSystem.scheduler.schedule(config.initialDelay, config.interval) {
        xsnService.getMerchantnodes().onComplete {
          case Success(Good(merchantnodes)) =>
            logger
              .info(s"Merchantnode information synced ${merchantnodes.length}")
            merchantnodeSynchronizerActor.ref ! MerchantnodeSynchronizerActor
              .UpdateMerchantnodes(merchantnodes)
          case Success(Bad(error)) =>
            logger.warn(
              s"Merchantnode information syncronization failed due to $error"
            )
          case Failure(exception) =>
            logger.error(
              s"Merchantnode information syncronization failed due to ${exception.getMessage}",
              exception
            )
        }
      }
    } else {
      logger.info("Disabled merchantnode synchronizer task")
    }
  }
}
