package com.xsn.explorer.services

import java.net.ConnectException

import akka.actor.Scheduler
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.config.{CoinMarketCapConfig, RetryConfig}
import com.xsn.explorer.errors._
import com.xsn.explorer.executors.ExternalServiceExecutionContext
import com.xsn.explorer.models.MarketInformation
import com.xsn.explorer.util.RetryableFuture
import javax.inject.Inject
import org.scalactic.{Bad, Good, One}
import org.scalactic.Accumulation._
import play.api.libs.ws.WSClient
import enumeratum.{Enum, EnumEntry}

import scala.util.{Failure, Success, Try}

sealed abstract class Currency(override val entryName: String) extends EnumEntry

object Currency extends Enum[Currency] {
  final case object USD extends Currency("USD")
  final case object BTC extends Currency("BTC")
  final case object EUR extends Currency("EUR")
  final case object GBP extends Currency("GBP")
  final case object JPY extends Currency("JPY")
  final case object MXN extends Currency("MXN")
  final case object NZD extends Currency("NZD")
  final case object TRY extends Currency("TRY")
  final case object UAH extends Currency("UAH")

  val values = findValues
}

trait CurrencyService {
  def getPrice(currency: Currency): FutureApplicationResult[BigDecimal]
  def getMarketInformation(): FutureApplicationResult[MarketInformation]
}

class CurrencyServiceCoinMarketCapImpl @Inject() (
    ws: WSClient,
    coinMarketCapConfig: CoinMarketCapConfig,
    retryConfig: RetryConfig
)(implicit
    ec: ExternalServiceExecutionContext,
    scheduler: Scheduler
) extends CurrencyService {

  private def requestFor(url: String) = {
    ws.url(s"${coinMarketCapConfig.host.string}/$url")
      .withHttpHeaders(
        "X-CMC_PRO_API_KEY" -> coinMarketCapConfig.key.string,
        "Accept" -> "application/json",
        "Accept-Encoding" -> "deflate, gzip"
      )
  }

  private def retrying[A](
      f: => FutureApplicationResult[A]
  ): FutureApplicationResult[A] = {
    val retry = RetryableFuture.withExponentialBackoff[ApplicationResult[A]](
      retryConfig.initialDelay,
      retryConfig.maxDelay
    )

    val shouldRetry: Try[ApplicationResult[A]] => Boolean = {
      case Success(Bad(One(CoinMarketCapRequestFailedError(500)))) => true
      case Success(Bad(One(CoinMarketCapRequestFailedError(502)))) => true
      case Success(Bad(One(CoinMarketCapRequestFailedError(503)))) => true
      case Success(Bad(One(CoinMarketCapRequestFailedError(504)))) => true
      case Failure(_: ConnectException)                            => true
      case _                                                       => false
    }

    retry(shouldRetry) {
      f
    }
  }

  override def getPrice(
      currency: Currency
  ): FutureApplicationResult[BigDecimal] = {
    retrying {
      val url =
        s"v1/tools/price-conversion?id=${coinMarketCapConfig.coinID.string}&amount=1&convert=${currency.entryName}"
      requestFor(url).get().map { response =>
        (response.status, response) match {
          case (200, r) =>
            Try(r.json).toOption
              .map { json =>
                (json \ "data" \ "quote" \ currency.entryName \ "price")
                  .asOpt[BigDecimal]
                  .map(Good(_))
                  .getOrElse(Bad(One(CoinMarketCapUnexpectedResponseError)))
              }
              .getOrElse(Bad(One(CoinMarketCapUnexpectedResponseError)))
          case (code, _) =>
            Bad(One(CoinMarketCapRequestFailedError(code)))
        }
      }
    }
  }

  override def getMarketInformation() = {
    retrying {
      val coinId = coinMarketCapConfig.coinID.string
      val url = s"v1/cryptocurrency/quotes/latest?id=$coinId"
      requestFor(url).get().map { response =>
        (response.status, response) match {
          case (200, r) =>
            Try(r.json).toOption
              .map { json =>
                val volume =
                  (json \ "data" \ coinId \ "quote" \ "USD" \ "volume_24h")
                    .asOpt[BigDecimal]
                    .map(Good(_))
                    .getOrElse(Bad(One(CoinMarketCapUnexpectedResponseError)))

                val marketcap =
                  (json \ "data" \ coinId \ "quote" \ "USD" \ "market_cap")
                    .asOpt[BigDecimal]
                    .map(Good(_))
                    .getOrElse(Bad(One(CoinMarketCapUnexpectedResponseError)))

                withGood(volume, marketcap)((volume, marketcap) =>
                  MarketInformation(volume, marketcap)
                )
              }
              .getOrElse(Bad(One(CoinMarketCapUnexpectedResponseError)))
          case (code, _) =>
            Bad(One(CoinMarketCapRequestFailedError(code)))
        }
      }
    }
  }
}
