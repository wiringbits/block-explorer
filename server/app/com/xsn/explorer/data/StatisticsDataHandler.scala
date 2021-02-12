package com.xsn.explorer.data

import java.time.Instant
import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.values.Address
import com.xsn.explorer.models.{AddressesReward, BlockRewardsSummary, Statistics}

import scala.language.higherKinds

trait StatisticsDataHandler[F[_]] {

  def getStatistics(): F[Statistics]

  def getRewardsSummary(numberOfBlocks: Int): F[BlockRewardsSummary]

  def getRewardedAddresses(startDate: Instant): F[AddressesReward]

  def getTPoSMerchantStakingAddresses(
      merchantAddress: Address
  ): F[List[Address]]
}

trait StatisticsBlockingDataHandler extends StatisticsDataHandler[ApplicationResult]
