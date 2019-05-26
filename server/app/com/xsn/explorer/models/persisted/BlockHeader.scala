package com.xsn.explorer.models.persisted

import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.values._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Json, Writes}

sealed trait BlockHeader extends Product with Serializable {

  def hash: Blockhash
  def previousBlockhash: Option[Blockhash]
  def merkleRoot: Blockhash
  def height: Height
  def time: Long

  def withFilter(filter: GolombCodedSet): BlockHeader.HasFilter = {
    this
      .into[BlockHeader.HasFilter]
      .withFieldConst(_.filter, filter)
      .transform
  }
}

object BlockHeader {

  case class Simple(
      hash: Blockhash,
      previousBlockhash: Option[Blockhash],
      merkleRoot: Blockhash,
      height: Height,
      time: Long
  ) extends BlockHeader

  case class HasFilter(
      hash: Blockhash,
      previousBlockhash: Option[Blockhash],
      merkleRoot: Blockhash,
      height: Height,
      time: Long,
      filter: GolombCodedSet
  ) extends BlockHeader

  private implicit val filterWrites: Writes[GolombCodedSet] = (obj: GolombCodedSet) => {
    Json.obj(
      "n" -> obj.n,
      "m" -> obj.m,
      "p" -> obj.p,
      "hex" -> obj.hex.string
    )
  }

  implicit val writes: Writes[BlockHeader] = (obj: BlockHeader) => {
    val filterMaybe = obj match {
      case x: HasFilter => Some(x.filter)
      case _ => Option.empty
    }

    Json.obj(
      "hash" -> obj.hash,
      "previousBlockhash" -> obj.previousBlockhash,
      "merkleRoot" -> obj.merkleRoot,
      "height" -> obj.height,
      "time" -> obj.time,
      "filter" -> filterMaybe
    )
  }
}
