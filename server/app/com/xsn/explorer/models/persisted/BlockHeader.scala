package com.xsn.explorer.models.persisted

import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.BlockExtractionMethod
import com.xsn.explorer.models.values._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Json, Writes}

sealed trait BlockHeader extends Product with Serializable {

  def hash: Blockhash
  def previousBlockhash: Option[Blockhash]
  def nextBlockhash: Option[Blockhash]
  def tposContract: Option[TransactionId]
  def merkleRoot: Blockhash
  def size: Size
  def height: Height
  def version: Int
  def time: Long
  def medianTime: Long
  def nonce: Long
  def bits: String
  def chainwork: String
  def difficulty: BigDecimal
  def extractionMethod: BlockExtractionMethod

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
      nextBlockhash: Option[Blockhash],
      tposContract: Option[TransactionId],
      merkleRoot: Blockhash,
      size: Size,
      height: Height,
      version: Int,
      time: Long,
      medianTime: Long,
      nonce: Long,
      bits: String,
      chainwork: String,
      difficulty: BigDecimal,
      extractionMethod: BlockExtractionMethod
  ) extends BlockHeader

  case class HasFilter(
      hash: Blockhash,
      previousBlockhash: Option[Blockhash],
      nextBlockhash: Option[Blockhash],
      tposContract: Option[TransactionId],
      merkleRoot: Blockhash,
      size: Size,
      height: Height,
      version: Int,
      time: Long,
      medianTime: Long,
      nonce: Long,
      bits: String,
      chainwork: String,
      difficulty: BigDecimal,
      extractionMethod: BlockExtractionMethod,
      filter: GolombCodedSet
  ) extends BlockHeader

  private implicit val filterWrites: Writes[GolombCodedSet] =
    (obj: GolombCodedSet) => {
      Json.obj(
        "n" -> obj.n,
        "m" -> obj.m,
        "p" -> obj.p,
        "hex" -> obj.hex.string
      )
    }

  val partialWrites: Writes[BlockHeader] = (obj: BlockHeader) => {
    val filterMaybe = obj match {
      case x: HasFilter => Some(x.filter)
      case _            => Option.empty
    }

    Json.obj(
      "hash" -> obj.hash,
      "previousBlockhash" -> obj.previousBlockhash,
      "merkleRoot" -> obj.merkleRoot,
      "height" -> obj.height,
      "time" -> obj.time,
      "version" -> obj.version,
      "nonce" -> obj.nonce,
      "bits" -> obj.bits,
      "filter" -> filterMaybe
    )
  }

  val completeWrites: Writes[BlockHeader] = (obj: BlockHeader) => {
    val filterMaybe = obj match {
      case x: HasFilter => Some(x.filter)
      case _            => Option.empty
    }

    Json.obj(
      "hash" -> obj.hash,
      "previousBlockhash" -> obj.previousBlockhash,
      "nextBlockhash" -> obj.nextBlockhash,
      "tposContract" -> obj.tposContract,
      "merkleRoot" -> obj.merkleRoot,
      "size" -> obj.size,
      "height" -> obj.height,
      "version" -> obj.version,
      "time" -> obj.time,
      "medianTime" -> obj.medianTime,
      "nonce" -> obj.nonce,
      "bits" -> obj.bits,
      "chainwork" -> obj.chainwork,
      "difficulty" -> obj.difficulty,
      "extractionMethod" -> obj.extractionMethod.entryName,
      "filter" -> filterMaybe
    )
  }
}
