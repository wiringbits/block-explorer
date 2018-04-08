package controllers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.helpers.DummyXSNService
import com.xsn.explorer.models.Height
import com.xsn.explorer.models.rpc.ServerStatistics
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.Good
import play.api.inject.bind
import play.api.test.Helpers._

import scala.concurrent.Future

class StatisticsControllerSpec extends MyAPISpec {

  val stats = ServerStatistics(Height(45454), 93548, BigDecimal("77645419.93177629"))

  val customXSNService = new DummyXSNService {
    override def getServerStatistics(): FutureApplicationResult[ServerStatistics] = {
      val result = Good(stats)
      Future.successful(result)
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(customXSNService))
      .build()

  "GET /stats" should {
    "return the server statistics" in {
      val response = GET("/stats")

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "height").as[Int] mustEqual stats.height.int
      (json \ "transactions").as[Int] mustEqual stats.transactions
      (json \ "totalSupply").as[BigDecimal] mustEqual stats.totalSupply
    }
  }
}
