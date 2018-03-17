package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class BlockRewards(coinstake: BlockReward, masternode: Option[BlockReward])

object BlockRewards {
  implicit val writes: Writes[BlockRewards] = Json.writes[BlockRewards]
}
