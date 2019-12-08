package com.xsn.explorer.services

import akka.actor.ActorSystem
import com.xsn.explorer.config.CoinMarketCapConfig.{CoinID, Host, Key}
import com.xsn.explorer.config.{CoinMarketCapConfig, RetryConfig}
import com.xsn.explorer.errors.{CoinMarketCapRequestFailedError, CoinMarketCapUnexpectedResponseError}
import com.xsn.explorer.helpers.Executors
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{mock => _, _}
import org.scalactic.{Bad, One}
import org.scalatest.MustMatchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.Future
import scala.concurrent.duration._

class CurrencyServiceCoinMarketCapImplSpec extends AsyncWordSpec with BeforeAndAfterAll {

  override def afterAll: Unit = {
    actorSystem.terminate()
    ()
  }

  val ws = mock[WSClient]
  val ec = Executors.externalServiceEC
  val actorSystem = ActorSystem()
  val scheduler = actorSystem.scheduler

  val coinMarketCapConfig = new CoinMarketCapConfig {
    override def host: CoinMarketCapConfig.Host = Host("host")

    override def key: CoinMarketCapConfig.Key = Key("key")

    override def coinID: CoinMarketCapConfig.CoinID = CoinID("123")
  }

  val retryConfig = RetryConfig(1.millisecond, 2.milliseconds)

  val request = mock[WSRequest]
  val response = mock[WSResponse]
  when(ws.url(anyString)).thenReturn(request)
  when(request.withHttpHeaders(any())).thenReturn(request)

  val service = new CurrencyServiceCoinMarketCapImpl(ws, coinMarketCapConfig, retryConfig)(ec, scheduler)

  def createSuccessfulUSDResponse(value: BigDecimal): String = {
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
       |      "USD": {
       |        "price": $value,
       |        "last_updated": "2019-08-30T18:51:11.000Z"
       |      }
       |    }
       |  }
       |}
     """.stripMargin
  }

  def createSuccessfulEURResponse(value: BigDecimal): String = {
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
       |      "EUR": {
       |        "price": $value,
       |        "last_updated": "2019-08-30T18:51:11.000Z"
       |      }
       |    }
       |  }
       |}
     """.stripMargin
  }

  "getUSDprice" should {
    "get coin price in usd" in {
      val responseBody = createSuccessfulUSDResponse(0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getUSDPrice) { result =>
        result.isGood mustEqual true

        val usd = result.get
        usd mustEqual 0.0734864351
      }
    }

    "fail when status is not 200" in {
      val responseBody = createSuccessfulUSDResponse(0.0734864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(502, json)

      whenReady(service.getUSDPrice) { result =>
        result mustEqual Bad(One(CoinMarketCapRequestFailedError(502)))
      }
    }

    "fail on unexpexted response" in {
      val json = Json.parse(s"""{\"USD\": ${0.0734864351}}""")

      mockRequest(request, response)(200, json)

      whenReady(service.getUSDPrice) { result =>
        result mustEqual Bad(One(CoinMarketCapUnexpectedResponseError))
      }
    }
  }

  "getEURprice" should {
    "get coin price in eur" in {
      val responseBody = createSuccessfulEURResponse(0.0634864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getEURPrice) { result =>
        result.isGood mustEqual true

        val eur = result.get
        eur mustEqual 0.0634864351
      }
    }

    "fail when status is not 200" in {
      val responseBody = createSuccessfulEURResponse(0.0634864351)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(502, json)

      whenReady(service.getEURPrice) { result =>
        result mustEqual Bad(One(CoinMarketCapRequestFailedError(502)))
      }
    }

    "fail on unexpexted response" in {
      val json = Json.parse(s"""{\"USD\": ${0.0634864351}}""")

      mockRequest(request, response)(200, json)

      whenReady(service.getEURPrice) { result =>
        result mustEqual Bad(One(CoinMarketCapUnexpectedResponseError))
      }
    }
  }

  private def mockRequest(request: WSRequest, response: WSResponse)(status: Int, body: JsValue) = {
    when(response.status).thenReturn(status)
    when(response.json).thenReturn(body)
    when(response.body).thenReturn(body.toString())
    when(request.get).thenReturn(Future.successful(response))
  }
}
