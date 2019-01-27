package com.xsn.explorer.models

import com.xsn.explorer.models.rpc.Block
import play.api.libs.json.{Json, Writes}

case class BlockDetails(block: Block, rewards: Option[BlockRewards])

object BlockDetails {

  implicit val writes: Writes[BlockDetails] = Json.writes[BlockDetails]

}
