package com.xsn.explorer.services

import java.net.ConnectException

import akka.actor.Scheduler
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.config.{CoinMarketCapConfig, RetryConfig}
import com.xsn.explorer.errors._
import com.xsn.explorer.executors.ExternalServiceExecutionContext
import com.xsn.explorer.util.RetryableFuture
import javax.inject.Inject
import org.scalactic.{Bad, Good, One}
import play.api.libs.ws.WSClient

import scala.util.{Failure, Success, Try}

trait CurrencyService {

  def getUSDPrice: FutureApplicationResult[BigDecimal]

  def getEURPrice: FutureApplicationResult[BigDecimal]

}

class CurrencyServiceCoinMarketCapImpl @Inject()(
    ws: WSClient,
    coinMarketCapConfig: CoinMarketCapConfig,
    retryConfig: RetryConfig
)(
    implicit ec: ExternalServiceExecutionContext,
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

  private def retrying[A](f: => FutureApplicationResult[A]): FutureApplicationResult[A] = {
    val retry = RetryableFuture.withExponentialBackoff[ApplicationResult[A]](
      retryConfig.initialDelay,
      retryConfig.maxDelay
    )

    val shouldRetry: Try[ApplicationResult[A]] => Boolean = {
      case Success(Bad(One(CoinMarketCapRequestFailedError(500)))) => true
      case Success(Bad(One(CoinMarketCapRequestFailedError(502)))) => true
      case Success(Bad(One(CoinMarketCapRequestFailedError(503)))) => true
      case Success(Bad(One(CoinMarketCapRequestFailedError(504)))) => true
      case Failure(_: ConnectException) => true
      case _ => false
    }

    retry(shouldRetry) {
      f
    }
  }

  override def getUSDPrice: FutureApplicationResult[BigDecimal] = {
    retrying {
      requestFor(s"v1/tools/price-conversion?id=${coinMarketCapConfig.coinID.string}&amount=1&convert=USD").get().map {
        response =>
          (response.status, response) match {
            case (200, r) =>
              Try(r.json).toOption
                .map { json =>
                  (json \ "data" \ "quote" \ "USD" \ "price")
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

  override def getEURPrice: FutureApplicationResult[BigDecimal] = {
    retrying {
      requestFor(s"v1/tools/price-conversion?id=${coinMarketCapConfig.coinID.string}&amount=1&convert=EUR").get().map {
        response =>
          (response.status, response) match {
            case (200, r) =>
              Try(r.json).toOption
                .map { json =>
                  (json \ "data" \ "quote" \ "EUR" \ "price")
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
}
