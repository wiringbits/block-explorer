package com.xsn.explorer.services.currencyService

import akka.actor.ActorSystem
import com.xsn.explorer.config.CoinMarketCapConfig.{CoinID, Host, Key}
import com.xsn.explorer.config.{CoinMarketCapConfig, RetryConfig}
import com.xsn.explorer.errors.{
  CoinMarketCapRequestFailedError,
  CoinMarketCapUnexpectedResponseError
}
import com.xsn.explorer.helpers.Executors
import com.xsn.explorer.services.{Currency, CurrencyServiceCoinMarketCapImpl}
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar._
import org.scalactic.{Bad, One}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.Future
import scala.concurrent.duration._

@com.github.ghik.silencer.silent
class GetPriceSpec extends AsyncWordSpec with BeforeAndAfterAll {

  override def afterAll: Unit = {
    actorSystem.terminate()
    ()
  }

  val ws = mock[WSClient]
  val ec = Executors.externalServiceEC
  val actorSystem = ActorSystem()
  val scheduler = actorSystem.scheduler

  val coinMarketCapConfig =
    CoinMarketCapConfig(Host("host"), Key("key"), CoinID("id"))

  val retryConfig = RetryConfig(1.millisecond, 2.milliseconds)

  val request = mock[WSRequest]
  val response = mock[WSResponse]
  when(ws.url(anyString)).thenReturn(request)
  when(request.withHttpHeaders(any())).thenReturn(request)

  val service =
    new CurrencyServiceCoinMarketCapImpl(ws, coinMarketCapConfig, retryConfig)(
      ec,
      scheduler
    )

  def createSuccessfullResponse(
      currency: Currency,
      value: BigDecimal
  ): String = {
    s"""
       |{
       |  "status": {
       |    "timestamp": "2019-12-16T01:58:23.659Z",
       |    "error_code": 0,
       |    "error_message": null,
       |    "elapsed": 6,
       |    "credit_count": 1
       |  },
       |  "data": {
       |    "id": 2633,
       |    "symbol": "XSN",
       |    "name": "Stakenet",
       |    "amount": 1,
       |    "last_updated": "2019-08-30T18:51:11.000Z",
       |    "quote": {
       |      "${currency.entryName}": {
       |        "price": $value,
       |        "last_updated": "2019-08-30T18:51:11.000Z"
       |      }
       |    }
       |  }
       |}
     """.stripMargin
  }

  "getPrice" should {
    "get coin price" in {
      val responseBody = createSuccessfullResponse(Currency.USD, 0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getPrice(Currency.USD)) { result =>
        result.isGood mustEqual true

        val usd = result.get
        usd mustEqual 0.0734864351
      }
    }

    "fail when status is not 200" in {
      val responseBody = createSuccessfullResponse(Currency.USD, 0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(502, json)

      whenReady(service.getPrice(Currency.USD)) { result =>
        result mustEqual Bad(One(CoinMarketCapRequestFailedError(502)))
      }
    }

    "fail on unexpexted response" in {
      val responseBody = createSuccessfullResponse(Currency.USD, 0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getPrice(Currency.BTC)) { result =>
        result mustEqual Bad(One(CoinMarketCapUnexpectedResponseError))
      }
    }
  }

  private def mockRequest(
      request: WSRequest,
      response: WSResponse
  )(status: Int, body: JsValue) = {
    when(response.status).thenReturn(status)
    when(response.json).thenReturn(body)
    when(response.body).thenReturn(body.toString())
    when(request.get).thenReturn(Future.successful(response))
  }
}
