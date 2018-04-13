package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.async.StatisticsFutureDataHandler
import com.xsn.explorer.models.Statistics

import scala.concurrent.ExecutionContext

class StatisticsService @Inject() (
    statisticsFutureDataHandler: StatisticsFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def getStatistics(): FutureApplicationResult[Statistics] = {
    statisticsFutureDataHandler.getStatistics()
  }
}
