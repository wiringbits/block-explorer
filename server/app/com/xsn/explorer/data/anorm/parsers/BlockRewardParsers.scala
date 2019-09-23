package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models.{BlockReward, RewardType}

object BlockRewardParsers {

  import CommonParsers._

  val parseValue = get[BigDecimal]("value")

  val parseType = str("type")
    .map(RewardType.withNameInsensitiveOption)
    .map { _.getOrElse(throw new RuntimeException("corrupted reward_type")) }

  val parseBlockReward = (parseAddress() ~ parseValue ~ parseType).map {
    case address ~ value ~ rewardType => (BlockReward(address, value), rewardType)
  }
}
