package controllers

import javax.inject.Inject

import com.xsn.explorer.services.StatisticsService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class StatisticsController @Inject() (
    statisticsService: StatisticsService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getStatus() = publicNoInput { _ =>
    statisticsService.getServerStatistics()
  }
}
