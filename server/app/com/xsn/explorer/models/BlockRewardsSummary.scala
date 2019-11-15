package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class BlockRewardsSummary(averageReward: BigDecimal, averageInput: BigDecimal, medianWaitTime: BigDecimal)

object BlockRewardsSummary {
  implicit val writes: Writes[BlockRewardsSummary] = Json.writes[BlockRewardsSummary]
}
