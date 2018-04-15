package com.xsn.explorer.tasks

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import akka.stream.alpakka.sqs.SqsSourceSettings
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.scaladsl.Sink
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.xsn.explorer.config.SeederConfig
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.processors.BlockEventsProcessor
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
class SQSSeederTask @Inject() (
    config: SeederConfig,
    blockEventsProcessor: BlockEventsProcessor,
    firstBlockSynchronizerTask: FirstBlockSynchronizerTask,
    backwardsSynchronizerTask: BackwardsSynchronizerTask)(
    implicit sqs: AmazonSQSAsync,
    materializer: Materializer) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val settings = SqsSourceSettings.Defaults.copy(maxBatchSize = 1, maxBufferSize = 1)

  if (config.isEnabled) {
    run()
  } else {
    logger.info("Seeder is disabled")
  }

  def run(): Unit = {
    logger.info("Starting seeder")

    SqsSource(config.queueUrl, settings)
        .runWith(Sink.foreach(handleMessage))
        .onComplete {
          case Failure(ex) =>
            logger.error("Failed to stream SQS messages", ex)

          case Success(_) =>
            logger.info("SQS stream completed")
        }
  }

  private def handleMessage(message: Message): Unit = {
    def onBlockhash(blockhash: Blockhash) = {
      val result = blockEventsProcessor.newLatestBlock(blockhash)

      result.recover {
        case NonFatal(ex) =>
          logger.error(s"Failed to process the latest block = ${blockhash.string}", ex)
      }

      result.foreach {
        case Bad(errors) =>
          logger.error(s"Failed to process the latest block = ${blockhash.string}, errors = $errors")

        case Good(_) =>
          logger.info(s"Block processed successfully = ${blockhash.string}")
          sqs.deleteMessageAsync(config.queueUrl, message.getReceiptHandle)
      }

      result.foreach {
        case Good(eventResult) => onBlockResult(eventResult)
        case _ => ()
      }
    }

    val body = message.getBody
    Blockhash
        .from(body)
        .orElse {
          logger.warn(s"Ignoring invalid message: $body")
          sqs.deleteMessageAsync(config.queueUrl, message.getReceiptHandle)
          None
        }
        .foreach(onBlockhash)
  }

  private def onBlockResult(eventResult: BlockEventsProcessor.Result) = eventResult match {
    case BlockEventsProcessor.FirstBlockCreated(_) =>
      firstBlockSynchronizerTask.sync()

    case BlockEventsProcessor.NewBlockAppended(_) =>
      firstBlockSynchronizerTask.sync()

    case BlockEventsProcessor.RechainDone(_, newBlock) =>
      firstBlockSynchronizerTask.sync()
      backwardsSynchronizerTask.sync(newBlock)

    case BlockEventsProcessor.MissingBlockProcessed(block) =>
      backwardsSynchronizerTask.sync(block)

    case BlockEventsProcessor.ExistingBlockIgnored(block) =>
      backwardsSynchronizerTask.sync(block)
  }
}
