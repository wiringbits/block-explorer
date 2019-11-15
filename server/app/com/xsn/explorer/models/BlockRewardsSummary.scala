package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}
import scala.math.BigDecimal.RoundingMode

case class BlockRewardsSummary(
    averageReward: BigDecimal,
    averageInput: BigDecimal,
    averagePoSInput: BigDecimal,
    averageTPoSInput: BigDecimal,
    medianWaitTime: BigDecimal
)

object BlockRewardsSummary {
  implicit val writes: Writes[BlockRewardsSummary] = (summary: BlockRewardsSummary) =>
    Json.obj(
      "averageReward" -> summary.averageReward.setScale(8, RoundingMode.HALF_UP),
      "averageInput" -> summary.averageInput.setScale(8, RoundingMode.HALF_UP),
      "averagePoSInput" -> summary.averagePoSInput.setScale(8, RoundingMode.HALF_UP),
      "averageTPoSInput" -> summary.averageTPoSInput.setScale(8, RoundingMode.HALF_UP),
      "medianWaitTime" -> summary.medianWaitTime.setScale(8, RoundingMode.HALF_UP)
    )
}
