package com.xsn.explorer.models

import enumeratum._

sealed abstract class BlockExtractionMethod(override val entryName: String)
    extends EnumEntry

object BlockExtractionMethod extends Enum[BlockExtractionMethod] {

  val values = findValues

  final case object ProofOfWork extends BlockExtractionMethod("PoW")
  final case object ProofOfStake extends BlockExtractionMethod("PoS")
  final case object TrustlessProofOfStake extends BlockExtractionMethod("TPoS")
}
