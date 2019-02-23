package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.values._
import play.api.libs.json.{Json, Writes}

case class BlockHeader(
    hash: Blockhash,
    previousBlockhash: Option[Blockhash],
    merkleRoot: Blockhash,
    height: Height,
    time: Long)

object BlockHeader {

  implicit val writes: Writes[BlockHeader] = Json.writes[BlockHeader]
}