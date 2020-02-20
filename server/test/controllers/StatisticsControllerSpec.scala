package controllers

import akka.actor.{Actor, ActorSystem, Props}
import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.StatisticsBlockingDataHandler
import com.xsn.explorer.errors.XSNUnexpectedResponseError
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}
import com.xsn.explorer.services.{Currency, XSNService}
import com.xsn.explorer.tasks.CurrencySynchronizerActor
import controllers.common.MyAPISpec
import org.mockito.Mockito.{mock => _, _}
import org.mockito.MockitoSugar.when
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar._
import play.api.inject.bind
import play.api.test.Helpers._

import scala.concurrent.Future

class StatisticsControllerSpec extends MyAPISpec with BeforeAndAfterAll {
  override def afterAll: Unit = {
    actorSystem.terminate()
    ()
  }

  val stats = Statistics(
    blocks = 45454,
    transactions = 93548,
    totalSupply = Some(BigDecimal("154516849.91650322")),
    circulatingSupply = Some(BigDecimal("78016849.91636708"))
  )

  val dataHandler = new StatisticsBlockingDataHandler {
    override def getStatistics(): ApplicationResult[Statistics] = Good(stats)

    override def getRewardsSummary(numberOfBlocks: Int) =
      Good(
        BlockRewardsSummary(
          BigDecimal("1000.12345678123"),
          BigDecimal("4800.12345678123"),
          BigDecimal("4900.12345678123"),
          BigDecimal("5000.12345678123"),
          BigDecimal("4500.12345678123"),
          BigDecimal("60000.12345678123"),
          BigDecimal("70000.12345678123")
        )
      )
  }

  val xsnService = mock[XSNService]
  val actorSystem = ActorSystem()

  override val application = guiceApplicationBuilder
    .overrides(bind[StatisticsBlockingDataHandler].to(dataHandler))
    .overrides(bind[XSNService].to(xsnService))
    .overrides(bind[ActorSystem].to(actorSystem))
    .build()

  class CurrencyActorMock extends Actor {
    override def receive: Receive = {
      case CurrencySynchronizerActor.GetPrices =>
        val reply: Map[Currency, BigDecimal] = Map(Currency.USD -> 0.071231351, Currency.BTC -> 0.063465494)
        sender() ! reply
    }
  }

  "GET /stats" should {
    "return the server statistics" in {
      val masternodes = 1000
      val difficulty = BigDecimal("129.1827211827212")
      when(xsnService.getMasternodeCount()).thenReturn(Future.successful(Good(masternodes)))
      when(xsnService.getDifficulty()).thenReturn(Future.successful(Good(difficulty)))

      val response = GET("/stats")

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "blocks").as[Int] mustEqual stats.blocks
      (json \ "transactions").as[Int] mustEqual stats.transactions
      (json \ "totalSupply").as[BigDecimal] mustEqual stats.totalSupply.get
      (json \ "circulatingSupply").as[BigDecimal] mustEqual stats.circulatingSupply.get
      (json \ "masternodes").as[Int] mustEqual masternodes
      (json \ "difficulty").as[BigDecimal] mustEqual difficulty
    }

    "return the stats even if getting masternodes throws an exception" in {
      val difficulty = BigDecimal("129.1827211827212")
      when(xsnService.getMasternodeCount()).thenReturn(Future.failed(new Exception))
      when(xsnService.getDifficulty()).thenReturn(Future.successful(Good(difficulty)))

      missingMasternodesTest(difficulty)
    }

    "return the stats even if the masternodes aren't available" in {
      val difficulty = BigDecimal("129.1827211827212")
      when(xsnService.getMasternodeCount()).thenReturn(Future.successful(Bad(XSNUnexpectedResponseError).accumulating))
      when(xsnService.getDifficulty()).thenReturn(Future.successful(Good(difficulty)))

      missingMasternodesTest(difficulty)
    }

    "return the stats even if getting the difficulty throws an exception" in {
      val masternodes = 1000
      when(xsnService.getMasternodeCount()).thenReturn(Future.successful(Good(masternodes)))
      when(xsnService.getDifficulty()).thenReturn(Future.failed(new Exception))

      missingDifficultyTest(masternodes)
    }

    "return the stats even if the difficulty isn't available" in {
      val masternodes = 1000
      when(xsnService.getMasternodeCount()).thenReturn(Future.successful(Good(masternodes)))
      when(xsnService.getDifficulty()).thenReturn(Future.successful(Bad(XSNUnexpectedResponseError).accumulating))

      missingDifficultyTest(masternodes)
    }
  }

  "GET /rewards-summary" should {
    "get rewards summary" in {
      val response = GET(s"/rewards-summary")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "averageReward").as[BigDecimal] mustEqual BigDecimal("1000.12345678")
      (json \ "averageInput").as[BigDecimal] mustEqual BigDecimal("4800.12345678")
      (json \ "medianInput").as[BigDecimal] mustEqual BigDecimal("4900.12345678")
      (json \ "averagePoSInput").as[BigDecimal] mustEqual BigDecimal("5000.12345678")
      (json \ "averageTPoSInput").as[BigDecimal] mustEqual BigDecimal("4500.12345678")
      (json \ "medianWaitTime").as[BigDecimal] mustEqual BigDecimal("60000.12345678")
      (json \ "averageWaitTime").as[BigDecimal] mustEqual BigDecimal("70000.12345678")
    }
  }

  "GET /prices" should {
    "get currency values" in {
      actorSystem.actorOf(Props(classOf[CurrencyActorMock], this), "currency_synchronizer")

      val response = GET("/prices")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "usd").as[BigDecimal] mustEqual 0.071231351
      (json \ "btc").as[BigDecimal] mustEqual 0.063465494
    }
  }

  private def missingMasternodesTest(difficulty: BigDecimal) = {
    val response = GET("/stats")

    status(response) mustEqual OK
    val json = contentAsJson(response)
    (json \ "blocks").as[Int] mustEqual stats.blocks
    (json \ "transactions").as[Int] mustEqual stats.transactions
    (json \ "totalSupply").as[BigDecimal] mustEqual stats.totalSupply.get
    (json \ "circulatingSupply").as[BigDecimal] mustEqual stats.circulatingSupply.get
    (json \ "difficulty").as[BigDecimal] mustEqual difficulty
  }

  private def missingDifficultyTest(masternodes: Int) = {
    val response = GET("/stats")

    status(response) mustEqual OK
    val json = contentAsJson(response)
    (json \ "blocks").as[Int] mustEqual stats.blocks
    (json \ "transactions").as[Int] mustEqual stats.transactions
    (json \ "totalSupply").as[BigDecimal] mustEqual stats.totalSupply.get
    (json \ "circulatingSupply").as[BigDecimal] mustEqual stats.circulatingSupply.get
    (json \ "masternodes").as[Int] mustEqual masternodes
  }
}
