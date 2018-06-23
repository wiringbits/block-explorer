package com.xsn.explorer.tasks

import javax.inject.Inject

import akka.actor.ActorSystem
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.processors.BlockEventsProcessor
import com.xsn.explorer.services.XSNService
import org.scalactic.Bad
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

class PollingSeederTask @Inject() (
    actorSystem: ActorSystem,
    xsnService: XSNService,
    blockEventsProcessor: BlockEventsProcessor,
    backwardsSynchronizerTask: BackwardsSynchronizerTask)(
    implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  def start() = {
    logger.info("Starting polling seeder task")
    actorSystem.scheduler.schedule(initialDelay = 15.seconds, interval = 1.minute)(run)
  }

  private def run(): Unit = {
    val result = for {
      block <- xsnService.getLatestBlock().toFutureOr
      result <- blockEventsProcessor.processBlock(block.hash).toFutureOr
    } yield onBlockResult(result)

    val _ = result
        .toFuture
        .map {
          case Bad(errors) => logger.error(s"Failed to sync latest block, errors = $errors")
          case _ => ()
        }
        .recover {
          case NonFatal(ex) => logger.error("Failed to sync latest block", ex)
        }
  }

  private def onBlockResult(eventResult: BlockEventsProcessor.Result) = eventResult match {
    case BlockEventsProcessor.FirstBlockCreated(block) =>
      backwardsSynchronizerTask.sync(block)

    case BlockEventsProcessor.RechainDone(_, newBlock) =>
      backwardsSynchronizerTask.sync(newBlock)

    case BlockEventsProcessor.MissingBlockProcessed(block) =>
      backwardsSynchronizerTask.sync(block)

    case BlockEventsProcessor.ReplacedByBlockHeight(newBlock) =>
      backwardsSynchronizerTask.sync(newBlock)

    case _ => ()
  }
}
