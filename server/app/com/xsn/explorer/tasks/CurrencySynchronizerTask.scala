package com.xsn.explorer.tasks

import akka.actor.{Actor, ActorSystem, Props}
import com.xsn.explorer.config.CurrencySynchronizerConfig
import com.xsn.explorer.services.{Currency, CurrencyService}
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CurrencySynchronizerActor extends Actor {
  import context._

  def receive = behavior(Currency.values.map(currency => currency -> BigDecimal(0)).toMap)

  private def behavior(prices: Map[Currency, BigDecimal]): Receive = {
    case CurrencySynchronizerActor.UpdatePrice(currency, price) =>
      val updatedPrices = prices + (currency -> price)
      become(behavior(updatedPrices))
    case CurrencySynchronizerActor.GetPrices =>
      sender() ! prices
  }
}

object CurrencySynchronizerActor {
  final case class UpdatePrice(currency: Currency, price: BigDecimal)
  final case class GetPrices()
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
      Currency.values.foreach { currency =>
        currencyService.getPrice(currency).onComplete {
          case Success(Good(price)) =>
            logger.info(s"${currency.entryName} price synced")
            currencySynchronizerActor ! CurrencySynchronizerActor.UpdatePrice(currency, price)
          case Success(Bad(error)) =>
            logger.info(s"${currency.entryName} price syncronization failed due to $error")
          case Failure(exception) =>
            logger.info(
              s"${currency.entryName} price syncronization failed due to ${exception.getLocalizedMessage}",
              exception
            )
        }
      }
    }
  }
}
