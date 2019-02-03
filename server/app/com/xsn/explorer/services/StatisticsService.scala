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
    val difficulty = xsnService.getDifficulty()

    val result = for {
      stats <- dbStats.toFutureOr
      count <- rpcStats.map {
        case Good(count) => Good(Some(count))
        case Bad(_) => Good(None)
      }.toFutureOr
      diff <- difficulty.map {
        case Good(difficulty) => Good(Some(difficulty))
        case Bad(_) => Good(None)
      }.toFutureOr
    } yield StatisticsDetails(stats, count, diff)

    result.toFuture
  }
}
