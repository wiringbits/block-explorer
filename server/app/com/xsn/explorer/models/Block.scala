package com.xsn.explorer.models

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
    previousBlockhash: Blockhash,
    nextBlockhash: Blockhash,
    merkleRoot: Blockhash,
    transactions: List[TransactionId],
    confirmations: Confirmations,
    size: Size,
    height: Height,
    version: Int,
    time: Long,
    medianTime: Long,
    nonce: Int,
    bits: String,
    chainwork: String,
    difficulty: BigDecimal
)

object Block {
  implicit val reads: Reads[Block] = {
    val builder = (__ \ 'hash).read[Blockhash] and
        (__ \ 'previousblockhash).read[Blockhash] and
        (__ \ 'nextblockhash).read[Blockhash] and
        (__ \ 'merkleroot).read[Blockhash] and
        (__ \ 'tx).read[List[TransactionId]] and
        (__ \ 'confirmations).read[Confirmations] and
        (__ \ 'size).read[Size] and
        (__ \ 'height).read[Height] and
        (__ \ 'version).read[Int] and
        (__ \ 'time).read[Long] and
        (__ \ 'mediantime).read[Long] and
        (__ \ 'nonce).read[Int] and
        (__ \ 'bits).read[String] and
        (__ \ 'chainwork).read[String] and
        (__ \ 'difficulty).read[BigDecimal]

    builder.apply { (hash, previous, next, root, transactions,
        confirmations, size, height, version, time,
        medianTime, nonce, bits, chainwork, difficulty) =>

      Block(
        hash, previous, next, root, transactions,
        confirmations, size, height, version, time,
        medianTime, nonce, bits, chainwork, difficulty)
    }
  }

  implicit val writes: Writes[Block] = Json.writes[Block]
}
