package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.async.StatisticsFutureDataHandler
import com.xsn.explorer.models.StatisticsDetails
import org.scalactic.{Bad, Good}

import scala.concurrent.ExecutionContext

class StatisticsService @Inject() (
    xsnService: XSNService,
    statisticsFutureDataHandler: StatisticsFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def getStatistics(): FutureApplicationResult[StatisticsDetails] = {
    val dbStats = statisticsFutureDataHandler.getStatistics()
    val rpcStats = xsnService.getMasternodeCount()

    val result = for {
      stats <- dbStats.toFutureOr
      count <- rpcStats.map {
        case Good(count) => Good(Some(count))
        case Bad(_) => Good(None)
      }.toFutureOr
      fakedStats = stats.copy(circulatingSupply = Some(70760408.694128), totalSupply = Some(80467288.2090169))
    } yield StatisticsDetails(fakedStats, count)

    result.toFuture
  }
}
