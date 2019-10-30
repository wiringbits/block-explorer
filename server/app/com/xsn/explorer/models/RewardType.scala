package com.xsn.explorer.models

import enumeratum._

sealed abstract class RewardType(override val entryName: String) extends EnumEntry

object RewardType extends Enum[RewardType] {

  val values = findValues

  final case object PoW extends RewardType("PoW")
  final case object PoS extends RewardType("PoS")
  final case object Masternode extends RewardType("MASTERNODE")
  final case object TPoSOwner extends RewardType("TPoS_OWNER")
  final case object TPoSMerchant extends RewardType("TPoS_MERCHANT")
}
