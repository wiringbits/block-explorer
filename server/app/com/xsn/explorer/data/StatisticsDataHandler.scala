package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.Statistics

import scala.language.higherKinds

trait StatisticsDataHandler[F[_]] {

  def getStatistics(): F[Statistics]
}

trait StatisticsBlockingDataHandler extends StatisticsDataHandler[ApplicationResult]
