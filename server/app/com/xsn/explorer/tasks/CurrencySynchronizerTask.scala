package com.xsn.explorer.tasks

import akka.actor.{Actor, ActorSystem, Props}
import com.xsn.explorer.config.CurrencySynchronizerConfig
import com.xsn.explorer.services.CurrencyService
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CurrencySynchronizerActor extends Actor {
  import context._

  def receive = behavior(0, 0)

  private def behavior(usd: BigDecimal, eur: BigDecimal): Receive = {
    case CurrencySynchronizerActor.UpdateUSD(updatedUSD) =>
      become(behavior(updatedUSD, eur))
    case CurrencySynchronizerActor.UpdateEUR(updatedEUR) =>
      become(behavior(usd, updatedEUR))
    case CurrencySynchronizerActor.GetCurrency =>
      val reply = (usd, eur)
      sender() ! reply
  }
}

object CurrencySynchronizerActor {
  final case class UpdateUSD(usd: BigDecimal)
  final case class UpdateEUR(eur: BigDecimal)
  final case class GetCurrency()
}

class CurrencySynchronizerTask @Inject()(
    config: CurrencySynchronizerConfig,
    actorSystem: ActorSystem,
    currencyService: CurrencyService
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  start()

  def start() = {
    logger.info("Starting currency synchronizer task")

    val currencySynchronizerActor = actorSystem.actorOf(Props[CurrencySynchronizerActor], "currency_synchronizer")
    actorSystem.scheduler.schedule(config.initialDelay, config.interval) {
      currencyService.getUSDPrice.onComplete {
        case Success(Good(usd)) =>
          logger.info(s"USD price synced to: $usd")
          currencySynchronizerActor ! CurrencySynchronizerActor.UpdateUSD(usd)
        case Success(Bad(error)) =>
          logger.info(s"USD syncronization failed due to $error")
        case Failure(exception) =>
          logger.info(s"USD syncronization failed due to $exception")
      }

      currencyService.getEURPrice.onComplete {
        case Success(Good(eur)) =>
          logger.info(s"EUR price synced to: $eur")
          currencySynchronizerActor ! CurrencySynchronizerActor.UpdateEUR(eur)
        case Success(Bad(error)) =>
          logger.info(s"EUR syncronization failed due to $error")
        case Failure(exception) =>
          logger.info(s"EUR syncronization failed due to $exception")
      }
    }
  }
}
