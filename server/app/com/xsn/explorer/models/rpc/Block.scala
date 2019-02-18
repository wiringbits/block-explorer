package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Represents a block coming from the RPC server.
 *
 * PoS blocks supported.
 * Pending to support PoW and TPoS blocks.
 */
case class Block(
    hash: Blockhash,
    previousBlockhash: Option[Blockhash], // first block doesn't have a previous block
    nextBlockhash: Option[Blockhash], // last block doesn't have a next block
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
    tposContract: Option[TransactionId]) {

  /**
   * Every block until 75 is PoW.
   */
  def isPoW: Boolean = height.int <= 75

  /**
   * From block 76, we have just PoW or TPoS
   */
  def isPoS: Boolean = !isPoW && tposContract.isEmpty

  /**
   * TPoS blocks need a tposcontract
   */
  def isTPoS: Boolean = !isPoW && tposContract.nonEmpty
}

object Block {

  implicit val reads: Reads[Block] = {
    val builder = (__ \ 'hash).read[Blockhash] and
        (__ \ 'previousblockhash).readNullable[Blockhash] and
        (__ \ 'nextblockhash).readNullable[Blockhash] and
        (__ \ 'merkleroot).read[Blockhash] and
        (__ \ 'tx).read[List[TransactionId]] and
        (__ \ 'confirmations).read[Confirmations] and
        (__ \ 'size).read[Size] and
        (__ \ 'height).read[Height] and
        (__ \ 'version).read[Int] and
        (__ \ 'time).read[Long] and
        (__ \ 'mediantime).read[Long] and
        (__ \ 'nonce).read[Long] and
        (__ \ 'bits).read[String] and
        (__ \ 'chainwork).read[String] and
        (__ \ 'difficulty).read[BigDecimal] and
        (__ \ 'tposcontract).readNullable[TransactionId]

    builder.apply { (hash, previous, next, root, transactions,
        confirmations, size, height, version, time,
        medianTime, nonce, bits, chainwork, difficulty, tposContract) =>

      Block(
        hash, previous, next, root, transactions,
        confirmations, size, height, version, time,
        medianTime, nonce, bits, chainwork, difficulty, tposContract)
    }
  }

  implicit val writes: Writes[Block] = Json.writes[Block]
}
