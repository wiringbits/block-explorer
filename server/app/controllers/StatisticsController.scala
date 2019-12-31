package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.services.StatisticsService
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json.{Json}

class StatisticsController @Inject()(statisticsService: StatisticsService, cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getStatus() = public { _ =>
    statisticsService.getStatistics()
    .toFutureOr
    .map {
      value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
    }
    .toFuture
  }

  def getBlockRewardsSummary() = public { _ =>
    statisticsService.getRewardsSummary(1000)
    .toFutureOr
    .map {
      value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
    }
    .toFuture
  }
}
