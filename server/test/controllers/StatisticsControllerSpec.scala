package controllers

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.StatisticsBlockingDataHandler
import com.xsn.explorer.errors.XSNUnexpectedResponseError
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{mock => _, _}
import org.mockito.MockitoSugar.when
import org.scalactic.{Bad, Good}
import org.scalatest.mockito.MockitoSugar._
import play.api.inject.bind
import play.api.test.Helpers._

import scala.concurrent.Future

class StatisticsControllerSpec extends MyAPISpec {

  val stats = Statistics(
    blocks = 45454,
    transactions = 93548,
    totalSupply = Some(BigDecimal("154516849.91650322")),
    circulatingSupply = Some(BigDecimal("78016849.91636708"))
  )

  val dataHandler = new StatisticsBlockingDataHandler {
    override def getStatistics(): ApplicationResult[Statistics] = Good(stats)

    override def getRewardsSummary(numberOfBlocks: Int) = Good(BlockRewardsSummary(1000, 5000, 60000))
  }

  val xsnService = mock[XSNService]

  override val application = guiceApplicationBuilder
    .overrides(bind[StatisticsBlockingDataHandler].to(dataHandler))
    .overrides(bind[XSNService].to(xsnService))
    .build()

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
      (json \ "averageReward").as[BigDecimal] mustEqual 1000
      (json \ "averageInput").as[BigDecimal] mustEqual 5000
      (json \ "medianWaitTime").as[BigDecimal] mustEqual 60000
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
