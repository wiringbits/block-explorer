package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.data.anorm.serializers.BlockRewardPostgresSerializer.{Reward, Stake}
import com.xsn.explorer.models.{BlockReward, BlockRewardsSummary, RewardType}

object BlockRewardParsers {

  import CommonParsers._

  val parseValue = get[BigDecimal]("value")

  val parseType = str("type")
    .map(RewardType.withNameInsensitiveOption)
    .map { _.getOrElse(throw new RuntimeException("corrupted reward_type")) }

  val parseStakedAmount = get[BigDecimal]("staked_amount")
  val parseStakedTime = get[Long]("staked_time")

  val parseBlockReward =
    (parseAddress() ~ parseValue ~ parseType ~ parseStakedAmount.? ~ parseStakedTime.?).map {
      case address ~ value ~ rewardType ~ stakedAmount ~ stakedTime => {
        val stake = for {
          stakedAmount <- stakedAmount
          stakedTime <- stakedTime
        } yield (Stake(stakedAmount, stakedTime))

        Reward(BlockReward(address, value), rewardType, stake)
      }
    }

  val parseSummary =
    (get[BigDecimal]("average_reward") ~
      get[BigDecimal]("average_input") ~
      get[BigDecimal]("pos_average_input") ~
      get[BigDecimal]("tpos_average_input") ~
      get[BigDecimal]("median_wait_time")).map {
      case averageReward ~ averageInput ~ averagePoSInput ~ averageTPoSInput ~ medianWaitTime =>
        BlockRewardsSummary(averageReward, averageInput, averagePoSInput, averageTPoSInput, medianWaitTime)
    }
}
