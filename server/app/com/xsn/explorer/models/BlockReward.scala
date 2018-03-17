package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class BlockReward(address: Address, value: BigDecimal)

object BlockReward {
  implicit val writes: Writes[BlockReward] = Json.writes[BlockReward]
}
