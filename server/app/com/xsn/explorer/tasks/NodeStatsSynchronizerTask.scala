package com.xsn.explorer.tasks

import akka.actor.{ActorSystem}
import com.xsn.explorer.config.NodeStatsSynchronizerConfig
import com.xsn.explorer.services.{StatisticsService}
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory
import com.xsn.explorer.services.synchronizer.NodeStatsSynchronizerActor

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class NodeStatsSynchronizerTask @Inject() (
    config: NodeStatsSynchronizerConfig,
    statisticsService: StatisticsService,
    actorSystem: ActorSystem,
    nodeStatsSynchronizerActor: NodeStatsSynchronizerActor.Ref
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  @com.github.ghik.silencer.silent
  def start() = {

    if (config.enabled) {
      logger.info("Starting node-stats synchronizer task")

      actorSystem.scheduler.schedule(config.initialDelay, config.interval) {
        statisticsService.getCoinsStaking().onComplete {
          case Success(Good(coinsStaking)) =>
            logger.info(s"CoinsStaking information synced ${coinsStaking}")
            nodeStatsSynchronizerActor.ref ! NodeStatsSynchronizerActor
              .UpdateCoinsStaking(coinsStaking)
          case Success(Bad(error)) =>
            logger.warn(
              s"CoinsStaking information syncronization failed due to $error"
            )
          case Failure(exception) =>
            logger.error(
              s"CoinsStaking information syncronization failed due to ${exception.getMessage}",
              exception
            )
        }
      }
    } else {
      logger.info("Disabled node-stats synchronizer task")
    }
  }
}
