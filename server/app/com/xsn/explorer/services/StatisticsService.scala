package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.async.BalanceFutureDataHandler
import com.xsn.explorer.models.Statistics

import scala.concurrent.ExecutionContext

class StatisticsService @Inject() (
    xsnService: XSNService,
    balanceFutureDataHandler: BalanceFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def getStatistics(): FutureApplicationResult[Statistics] = {
    val result = for {
      server <- xsnService.getServerStatistics().toFutureOr
      circulatingSupply <- balanceFutureDataHandler.getCirculatingSupply().toFutureOr
    } yield Statistics(server.height, server.transactions, server.totalSupply, circulatingSupply)

    result.toFuture
  }
}
