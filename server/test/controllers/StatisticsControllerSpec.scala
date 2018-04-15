package controllers

import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.data.StatisticsBlockingDataHandler
import com.xsn.explorer.helpers.DummyXSNService
import com.xsn.explorer.models.Statistics
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.Good
import play.api.inject.bind
import play.api.test.Helpers._

import scala.concurrent.Future

class StatisticsControllerSpec extends MyAPISpec {

  val stats = Statistics(
    blocks = 45454,
    transactions = 93548,
    totalSupply = Some(BigDecimal("154516849.91650322")),
    circulatingSupply = Some(BigDecimal("78016849.91636708")))

  val dataHandler = new StatisticsBlockingDataHandler {
    override def getStatistics(): ApplicationResult[Statistics] = Good(stats)
  }

  val xsnService = new DummyXSNService {
    override def getMasternodeCount(): FutureApplicationResult[Int] = {
      Future.successful(Good(1000))
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[StatisticsBlockingDataHandler].to(dataHandler))
      .overrides(bind[XSNService].to(xsnService))
      .build()

  "GET /stats" should {
    "return the server statistics" in {
      val response = GET("/stats")

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "blocks").as[Int] mustEqual stats.blocks
      (json \ "transactions").as[Int] mustEqual stats.transactions
      (json \ "totalSupply").as[BigDecimal] mustEqual stats.totalSupply
      (json \ "circulatingSupply").as[BigDecimal] mustEqual stats.circulatingSupply
      (json \ "masternodes").as[Int] mustEqual 1000
    }
  }
}
