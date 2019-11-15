package controllers

import com.xsn.explorer.services.StatisticsService
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject

class StatisticsController @Inject()(statisticsService: StatisticsService, cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getStatus() = public { _ =>
    statisticsService.getStatistics()
  }

  def getBlockRewardsSummary() = public { _ =>
    statisticsService.getRewardsSummary(1000)
  }
}
