package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.models.rpc.ServerStatistics

class StatisticsService @Inject() (xsnService: XSNService) {

  def getServerStatistics(): FutureApplicationResult[ServerStatistics] = {
    xsnService.getServerStatistics()
  }
}
