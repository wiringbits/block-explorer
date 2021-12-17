package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/** Represents a block coming from the RPC server.
  */
sealed trait Block[Tx] {
  def hash: Blockhash
  def previousBlockhash: Option[Blockhash] // first block doesn't have a previous block
  def nextBlockhash: Option[Blockhash] // last block doesn't have a next block
  def merkleRoot: Blockhash
  def transactions: List[Tx]
  def confirmations: Confirmations
  def size: Size
  def height: Height
  def version: Int
  def time: Long
  def medianTime: Long
  def nonce: Long
  def bits: String
  def chainwork: String
  def difficulty: BigDecimal
  def tposContract: Option[TransactionId]
}

object Block {

  case class Canonical(
      hash: Blockhash,
      previousBlockhash: Option[Blockhash],
      nextBlockhash: Option[Blockhash],
      merkleRoot: Blockhash,
      transactions: List[TransactionId],
      confirmations: Confirmations,
      size: Size,
      height: Height,
      version: Int,
      time: Long,
      medianTime: Long,
      nonce: Long,
      bits: String,
      chainwork: String,
      difficulty: BigDecimal,
      tposContract: Option[TransactionId]
  ) extends Block[TransactionId]

  case class HasTransactions[VIN <: TransactionVIN](
      hash: Blockhash,
      previousBlockhash: Option[Blockhash],
      nextBlockhash: Option[Blockhash],
      merkleRoot: Blockhash,
      transactions: List[Transaction[VIN]],
      confirmations: Confirmations,
      size: Size,
      height: Height,
      version: Int,
      time: Long,
      medianTime: Long,
      nonce: Long,
      bits: String,
      chainwork: String,
      difficulty: BigDecimal,
      tposContract: Option[TransactionId]
  ) extends Block[Transaction[VIN]]

  implicit val canonicalReads: Reads[Block.Canonical] = {
    val builder = (__ \ Symbol("hash")).read[Blockhash] and
      (__ \ Symbol("previousblockhash")).readNullable[Blockhash] and
      (__ \ Symbol("nextblockhash")).readNullable[Blockhash] and
      (__ \ Symbol("merkleroot")).read[Blockhash] and
      (__ \ Symbol("tx")).read[List[TransactionId]] and
      (__ \ Symbol("confirmations")).read[Confirmations] and
      (__ \ Symbol("size")).read[Size] and
      (__ \ Symbol("height")).read[Height] and
      (__ \ Symbol("version")).read[Int] and
      (__ \ Symbol("time")).read[Long] and
      (__ \ Symbol("mediantime")).read[Long] and
      (__ \ Symbol("nonce")).read[Long] and
      (__ \ Symbol("bits")).read[String] and
      (__ \ Symbol("chainwork")).read[String] and
      (__ \ Symbol("difficulty")).read[BigDecimal] and
      (__ \ Symbol("tposcontract")).readNullable[TransactionId]

    builder.apply {
      (
          hash,
          previous,
          next,
          root,
          transactions,
          confirmations,
          size,
          height,
          version,
          time,
          medianTime,
          nonce,
          bits,
          chainwork,
          difficulty,
          tposContract
      ) =>
        Canonical(
          hash,
          previous,
          next,
          root,
          transactions,
          confirmations,
          size,
          height,
          version,
          time,
          medianTime,
          nonce,
          bits,
          chainwork,
          difficulty,
          tposContract
        )
    }
  }

  implicit val canonicalWrites: Writes[Canonical] = Json.writes[Canonical]

  implicit val hasTransactionsReads: Reads[HasTransactions[TransactionVIN]] =
    (json: JsValue) => {
      def readTransactions(implicit
          reads: Reads[Transaction[TransactionVIN]]
      ) = {
        (json \ "tx").validate[List[Transaction[TransactionVIN]]]
      }

      for {
        hash <- (json \ "hash").validate[Blockhash]
        previousBlockhash <- (json \ "previousblockhash").validateOpt[Blockhash]
        nextBlockhash <- (json \ "nextblockhash").validateOpt[Blockhash]
        merkleRoot <- (json \ "merkleroot").validate[Blockhash]
        confirmations <- (json \ "confirmations").validate[Confirmations]
        height <- (json \ "height").validate[Height]
        size <- (json \ "size").validate[Size]
        version <- (json \ "version").validate[Int]
        time <- (json \ "time").validate[Long]
        medianTime <- (json \ "mediantime").validate[Long]
        nonce <- (json \ "nonce").validate[Long]
        bits <- (json \ "bits").validate[String]
        chainwork <- (json \ "chainwork").validate[String]
        difficulty <- (json \ "difficulty").validate[BigDecimal]
        tposContract <- (json \ "tposcontract").validateOpt[TransactionId]

        txReads = Transaction.batchReads(hash, confirmations, time)
        transactions <- readTransactions(txReads)
      } yield Block.HasTransactions(
        hash,
        previousBlockhash,
        nextBlockhash,
        merkleRoot,
        transactions,
        confirmations,
        size,
        height,
        version,
        time,
        medianTime,
        nonce,
        bits,
        chainwork,
        difficulty,
        tposContract
      )
    }
}
