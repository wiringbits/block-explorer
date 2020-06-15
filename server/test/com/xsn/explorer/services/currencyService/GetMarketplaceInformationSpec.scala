package com.xsn.explorer.services.currencyService

import akka.actor.ActorSystem
import com.xsn.explorer.config.CoinMarketCapConfig.{CoinID, Host, Key}
import com.xsn.explorer.config.{CoinMarketCapConfig, RetryConfig}
import com.xsn.explorer.errors.{CoinMarketCapRequestFailedError, CoinMarketCapUnexpectedResponseError}
import com.xsn.explorer.helpers.Executors
import com.xsn.explorer.services.CurrencyServiceCoinMarketCapImpl
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

class GetMarketplaceInformationSpec extends AsyncWordSpec with BeforeAndAfterAll {

  override def afterAll: Unit = {
    actorSystem.terminate()
    ()
  }

  val ws = mock[WSClient]
  val ec = Executors.externalServiceEC
  val actorSystem = ActorSystem()
  val scheduler = actorSystem.scheduler

  val coinMarketCapConfig = CoinMarketCapConfig(Host("host"), Key("key"), CoinID("id"))

  val retryConfig = RetryConfig(1.millisecond, 2.milliseconds)

  val request = mock[WSRequest]
  val response = mock[WSResponse]
  when(ws.url(anyString)).thenReturn(request)
  when(request.withHttpHeaders(any())).thenReturn(request)

  val service = new CurrencyServiceCoinMarketCapImpl(ws, coinMarketCapConfig, retryConfig)(ec, scheduler)

  def createSuccessfullResponse(volume: Option[BigDecimal], marketcap: Option[BigDecimal]): String = {
    val volumeString = volume.map(v => s""""volume_24h": $v,""").getOrElse("")
    val marketcapString = marketcap.map(m => s""""market_cap": $m,""").getOrElse("")

    s"""
       |{
       |  "status": {
       |    "timestamp": "2020-06-06T18:00:31.933Z",
       |    "error_code": 0,
       |    "error_message": null,
       |    "elapsed": 8,
       |    "credit_count": 1,
       |    "notice": null
       |  },
       |  "data": {
       |    "${coinMarketCapConfig.coinID.string}": {
       |      "id": 2633,
       |      "name": "Stakenet",
       |      "symbol": "XSN",
       |      "slug": "stakenet",
       |      "num_market_pairs": 2,
       |      "date_added": "2018-04-11T00:00:00.000Z",
       |      "tags": [],
       |      "max_supply": null,
       |      "circulating_supply": 99751243.5782932,
       |      "total_supply": 106891534.497866,
       |      "is_active": 1,
       |      "platform": null,
       |      "cmc_rank": 522,
       |      "is_fiat": 0,
       |      "last_updated": "2020-06-06T17:59:08.000Z",
       |      "quote": {
       |        "USD": {
       |          "price": 0.0517564054195,
       |          $volumeString
       |          "percent_change_1h": 2.18132,
       |          "percent_change_24h": 2.75627,
       |          "percent_change_7d": -2.05932,
       |          $marketcapString
       |          "last_updated": "2020-06-06T17:59:08.000Z"
       |        }
       |      }
       |    }
       |  }
       |}
     """.stripMargin
  }

  "getPrice" should {
    "get coin price" in {
      val volume = BigDecimal(65123)
      val marketcap = BigDecimal(5123456)
      val responseBody = createSuccessfullResponse(Some(volume), Some(marketcap))
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getMarketInformation()) { result =>
        result.isGood mustEqual true

        val marketInformation = result.get
        marketInformation.volume mustBe volume
        marketInformation.marketcap mustBe marketcap
      }
    }

    "fail when status is not 200" in {
      val volume = BigDecimal(65123)
      val marketcap = BigDecimal(5123456)
      val responseBody = createSuccessfullResponse(Some(volume), Some(marketcap))
      val json = Json.parse(responseBody)

      mockRequest(request, response)(502, json)

      whenReady(service.getMarketInformation()) { result =>
        result mustEqual Bad(One(CoinMarketCapRequestFailedError(502)))
      }
    }

    "fail on when volume is missing" in {
      val marketcap = BigDecimal(5123456)
      val responseBody = createSuccessfullResponse(None, Some(marketcap))
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getMarketInformation()) { result =>
        result mustEqual Bad(One(CoinMarketCapUnexpectedResponseError))
      }
    }

    "fail on when marketcap is missing" in {
      val volume = BigDecimal(65123)
      val responseBody = createSuccessfullResponse(Some(volume), None)
      val json = Json.parse(responseBody)

      mockRequest(request, response)(200, json)

      whenReady(service.getMarketInformation()) { result =>
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
