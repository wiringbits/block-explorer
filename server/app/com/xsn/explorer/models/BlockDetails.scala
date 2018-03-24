package com.xsn.explorer.models

import com.xsn.explorer.models.rpc.Block
import play.api.libs.json.{Json, Writes}

case class BlockDetails(block: Block, rewards: BlockRewards)

object BlockDetails {

  implicit val reads: Writes[BlockDetails] = Json.writes[BlockDetails]

}
