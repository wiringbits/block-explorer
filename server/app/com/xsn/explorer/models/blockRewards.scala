package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

sealed trait BlockRewards

object BlockRewards {

  implicit val writes: Writes[BlockRewards] = Writes[BlockRewards] {
    case r: PoWBlockRewards => Json.writes[PoWBlockRewards].writes(r)
    case r: PoSBlockRewards => Json.writes[PoSBlockRewards].writes(r)
    case r: TPoSBlockRewards => Json.writes[TPoSBlockRewards].writes(r)
  }
}

case class PoWBlockRewards(reward: BlockReward) extends BlockRewards

case class PoSBlockRewards(
    coinstake: BlockReward,
    masternode: Option[BlockReward],
    treasury: Option[BlockReward],
    stakedAmount: BigDecimal,
    stakedDuration: Long
) extends BlockRewards

case class TPoSBlockRewards(
    owner: BlockReward,
    merchant: BlockReward,
    masternode: Option[BlockReward],
    treasury: Option[BlockReward],
    stakedAmount: BigDecimal,
    stakedDuration: Long
) extends BlockRewards
