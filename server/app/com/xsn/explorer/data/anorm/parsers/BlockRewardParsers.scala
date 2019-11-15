package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
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
      case address ~ value ~ rewardType ~ stakedAmount ~ stakedTime =>
        (BlockReward(address, value), rewardType, stakedAmount, stakedTime)
    }

  val parseSummary =
    (get[BigDecimal]("average_reward") ~ get[BigDecimal]("average_input") ~ get[BigDecimal]("median_wait_time")).map {
      case averageReward ~ averageInput ~ medianWaitTime =>
        BlockRewardsSummary(averageReward, averageInput, medianWaitTime)
    }
}
