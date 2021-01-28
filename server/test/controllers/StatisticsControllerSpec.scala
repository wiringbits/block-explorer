package controllers

import java.time.Instant

import akka.actor.{Actor, ActorSystem, Props}
import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.StatisticsBlockingDataHandler
import com.xsn.explorer.errors.XSNUnexpectedResponseError
import com.xsn.explorer.models.{BlockRewardsSummary, MarketInformation, MarketStatistics, Statistics, NodeStatistics}
import com.xsn.explorer.services.{Currency, XSNService}
import com.xsn.explorer.tasks.CurrencySynchronizerActor
import com.xsn.explorer.services.synchronizer.repository.{
  MasternodeRepository,
  MerchantnodeRepository,
  NodeStatsRepository
}
import controllers.common.MyAPISpec
import org.mockito.MockitoSugar.{when, _}
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfterAll
import com.xsn.explorer.models.values.Address
import play.api.inject.bind
import play.api.libs.json.Json
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

  val nodeStats = NodeStatistics(
    masternodes = 1000,
    enabledMasternodes = 900,
    masternodesProtocols = Map("70209" -> 950, "70210" -> 50),
    tposnodes = 100,
    enabledTposnodes = 90,
    tposnodesProtocols = Map("70209" -> 90),
    coinsStaking = 100
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

    override def getTPoSMerchantStakingAddresses(
        address: Address
    ): ApplicationResult[List[Address]] = Good(List(Address.from("123").get))

    override def getRewardedAddressesCount(startDate: Instant): ApplicationResult[Long] = Good(123L)
  }

  val xsnService = mock[XSNService]
  val masternodeRepository = mock[MasternodeRepository]
  val merchantnodeRepository = mock[MerchantnodeRepository]
  val nodeStatsRepository = mock[NodeStatsRepository]
  val actorSystem = ActorSystem()
  actorSystem.actorOf(
    Props(classOf[CurrencyActorMock], this),
    "currency_synchronizer"
  )

  override val application = guiceApplicationBuilder
    .overrides(bind[StatisticsBlockingDataHandler].to(dataHandler))
    .overrides(bind[XSNService].to(xsnService))
    .overrides(bind[MasternodeRepository].to(masternodeRepository))
    .overrides(bind[MerchantnodeRepository].to(merchantnodeRepository))
    .overrides(bind[NodeStatsRepository].to(nodeStatsRepository))
    .overrides(bind[ActorSystem].to(actorSystem))
    .build()

  class CurrencyActorMock extends Actor {

    override def receive: Receive = { case CurrencySynchronizerActor.GetMarketStatistics =>
      val map: Map[Currency, BigDecimal] =
        Map(Currency.USD -> 0.071231351, Currency.BTC -> 0.063465494)
      val reply = MarketStatistics(map, MarketInformation(0, 0))
      sender() ! reply
    }
  }

  "GET /stats" should {
    "return the server statistics" in {
      val masternodes = 1000
      val tposnodes = 100
      val difficulty = BigDecimal("129.1827211827212")
      when(masternodeRepository.getCount())
        .thenReturn(Future.successful(masternodes))
      when(masternodeRepository.getAll()).thenReturn(Future.successful(List()))
      when(merchantnodeRepository.getCount())
        .thenReturn(Future.successful(tposnodes))
      when(merchantnodeRepository.getAll())
        .thenReturn(Future.successful(List()))
      when(xsnService.getDifficulty())
        .thenReturn(Future.successful(Good(difficulty)))

      val response = GET("/stats")

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "blocks").as[Int] mustEqual stats.blocks
      (json \ "transactions").as[Int] mustEqual stats.transactions
      (json \ "totalSupply").as[BigDecimal] mustEqual stats.totalSupply.get
      (json \ "circulatingSupply")
        .as[BigDecimal] mustEqual stats.circulatingSupply.get
      (json \ "masternodes").as[Int] mustEqual masternodes
      (json \ "tposnodes").as[Int] mustEqual tposnodes
      (json \ "difficulty").as[BigDecimal] mustEqual difficulty
    }

    "return the stats even if getting the difficulty throws an exception" in {
      val masternodes = 1000
      val tposnodes = 100
      when(masternodeRepository.getCount())
        .thenReturn(Future.successful(masternodes))
      when(merchantnodeRepository.getCount())
        .thenReturn(Future.successful(tposnodes))
      when(xsnService.getDifficulty()).thenReturn(Future.failed(new Exception))

      missingDifficultyTest(masternodes)
    }

    "return the stats even if the difficulty isn't available" in {
      val masternodes = 1000
      val tposnodes = 100
      when(masternodeRepository.getCount())
        .thenReturn(Future.successful(masternodes))
      when(merchantnodeRepository.getCount())
        .thenReturn(Future.successful(tposnodes))
      when(xsnService.getDifficulty()).thenReturn(
        Future.successful(Bad(XSNUnexpectedResponseError).accumulating)
      )

      missingDifficultyTest(masternodes)
    }
  }

  "GET /node-stats" should {
    "return the node statistics" in {
      val masternodes = 1000
      val enabledMasternodes = 900
      val mnProtocols = Map("70209" -> 950, "70210" -> 50)
      val tposnodes = 100
      val enabledTposnodes = 90
      val tnProtocols = Map("70209" -> 90)
      val coinsStaking = 100

      when(masternodeRepository.getCount())
        .thenReturn(Future.successful(masternodes))
      when(masternodeRepository.getEnabledCount())
        .thenReturn(Future.successful(enabledMasternodes))
      when(masternodeRepository.getProtocols())
        .thenReturn(Future.successful(mnProtocols))
      when(merchantnodeRepository.getCount())
        .thenReturn(Future.successful(tposnodes))
      when(merchantnodeRepository.getEnabledCount())
        .thenReturn(Future.successful(enabledTposnodes))
      when(merchantnodeRepository.getProtocols())
        .thenReturn(Future.successful(tnProtocols))
      when(nodeStatsRepository.getCoinsStaking())
        .thenReturn(Future.successful(coinsStaking))

      val response = GET("/node-stats")

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "masternodes").as[Int] mustEqual masternodes
      (json \ "enabledMasternodes").as[Int] mustEqual enabledMasternodes
      (json \ "masternodesProtocols").as[Map[String, Int]] mustEqual mnProtocols
      (json \ "tposnodes").as[Int] mustEqual tposnodes
      (json \ "enabledTposnodes").as[Int] mustEqual enabledTposnodes
      (json \ "tposnodesProtocols").as[Map[String, Int]] mustEqual tnProtocols
    }
  }

  "GET /rewards-summary" should {
    "get rewards summary" in {
      val response = GET(s"/rewards-summary")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "averageReward").as[BigDecimal] mustEqual BigDecimal(
        "1000.12345678"
      )
      (json \ "averageInput").as[BigDecimal] mustEqual BigDecimal(
        "4800.12345678"
      )
      (json \ "medianInput").as[BigDecimal] mustEqual BigDecimal(
        "4900.12345678"
      )
      (json \ "averagePoSInput").as[BigDecimal] mustEqual BigDecimal(
        "5000.12345678"
      )
      (json \ "averageTPoSInput").as[BigDecimal] mustEqual BigDecimal(
        "4500.12345678"
      )
      (json \ "medianWaitTime").as[BigDecimal] mustEqual BigDecimal(
        "60000.12345678"
      )
      (json \ "averageWaitTime").as[BigDecimal] mustEqual BigDecimal(
        "70000.12345678"
      )
      (json \ "rewardedAddressesCountLast72Hours").as[BigDecimal] mustEqual 123L
    }
  }

  "GET /prices" should {
    "get currency values" in {
      val response = GET("/prices")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "usd").as[BigDecimal] mustEqual 0.071231351
      (json \ "btc").as[BigDecimal] mustEqual 0.063465494
    }

    "get a specific currency" in {
      val response = GET("/prices?currency=usd")
      status(response) mustEqual OK

      val expected = Json.obj("usd" -> 0.071231351)
      val json = contentAsJson(response)

      json mustEqual expected
    }

    "getting a specific currency is case insensitive" in {
      val response = GET("/prices?currency=USD")
      status(response) mustEqual OK

      val expected = Json.obj("usd" -> 0.071231351)
      val json = contentAsJson(response)

      json mustEqual expected
    }

    "return not found when currency does not exist" in {
      val response = GET("/prices?currency=asd")
      status(response) mustEqual NOT_FOUND
    }
  }

  private def missingDifficultyTest(masternodes: Int) = {
    val response = GET("/stats")

    status(response) mustEqual OK
    val json = contentAsJson(response)
    (json \ "blocks").as[Int] mustEqual stats.blocks
    (json \ "transactions").as[Int] mustEqual stats.transactions
    (json \ "totalSupply").as[BigDecimal] mustEqual stats.totalSupply.get
    (json \ "circulatingSupply")
      .as[BigDecimal] mustEqual stats.circulatingSupply.get
    (json \ "masternodes").as[Int] mustEqual masternodes
  }
}
