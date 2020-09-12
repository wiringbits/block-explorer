package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}
import com.xsn.explorer.models.values.Address

import scala.language.higherKinds

trait StatisticsDataHandler[F[_]] {

  def getStatistics(): F[Statistics]

  def getRewardsSummary(numberOfBlocks: Int): F[BlockRewardsSummary]

  def getTPoSMerchantStakingAddresses(merchantAddress: Address): F[List[Address]]
}

trait StatisticsBlockingDataHandler extends StatisticsDataHandler[ApplicationResult]
