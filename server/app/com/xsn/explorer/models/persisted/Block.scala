package com.xsn.explorer.models.persisted

import com.xsn.explorer.models._
import com.xsn.explorer.models.values.{Blockhash, Height}
import enumeratum._

case class Block(
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
    extractionMethod: Block.ExtractionMethod)

object Block {

  sealed abstract class ExtractionMethod(override val entryName: String) extends EnumEntry
  object ExtractionMethod extends Enum[ExtractionMethod] {

    val values = findValues

    final case object ProofOfWork extends ExtractionMethod("PoW")
    final case object ProofOfStake extends ExtractionMethod("PoS")
    final case object TrustlessProofOfStake extends ExtractionMethod("TPoS")
  }
}
