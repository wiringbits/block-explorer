package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}

import scala.language.higherKinds

trait StatisticsDataHandler[F[_]] {

  def getStatistics(): F[Statistics]

  def getRewardsSummary(numberOfBlocks: Int): F[BlockRewardsSummary]
}

trait StatisticsBlockingDataHandler extends StatisticsDataHandler[ApplicationResult]
