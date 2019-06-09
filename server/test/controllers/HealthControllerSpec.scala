package controllers

import com.xsn.explorer.data.StatisticsBlockingDataHandler
import com.xsn.explorer.helpers.DataGenerator
import com.xsn.explorer.models._
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.mockito.MockitoSugar._
import org.scalactic.Good
import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers._

import scala.concurrent.Future

class HealthControllerSpec extends MyAPISpec {

  private val xsnServiceMock = mock[XSNService]
  private val statisticsDataHandlerMock = mock[StatisticsBlockingDataHandler]

  val application: Application = guiceApplicationBuilder
    .overrides(bind[XSNService].to(xsnServiceMock))
    .overrides(bind[StatisticsBlockingDataHandler].to(statisticsDataHandlerMock))
    .build()

  "GET /health" should {
    "return OK" in {
      val latestXSNBlock = DataGenerator.randomBlock().copy(height = Height(19))
      val stats = Statistics(10, 0, None, None)
      when(xsnServiceMock.getLatestBlock()).thenReturn(Future.successful(Good(latestXSNBlock)))
      when(statisticsDataHandlerMock.getStatistics()).thenReturn(Good(stats))
      val response = GET("/health")

      status(response) mustEqual OK
    }

    "return Internal Server Error if there are 10 missing blocks" in {
      val latestXSNBlock = DataGenerator.randomBlock().copy(height = Height(20))
      val stats = Statistics(10, 0, None, None)
      when(xsnServiceMock.getLatestBlock()).thenReturn(Future.successful(Good(latestXSNBlock)))
      when(statisticsDataHandlerMock.getStatistics()).thenReturn(Good(stats))
      val response = GET("/health")

      status(response) mustEqual INTERNAL_SERVER_ERROR
    }
  }
}
