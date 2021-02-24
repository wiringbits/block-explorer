package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{Address, HexString, TransactionId}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

sealed trait TransactionVIN {
  def txid: TransactionId
  def voutIndex: Int

  def withValues(
      value: BigDecimal,
      addresses: List[Address],
      pubKeyScript: HexString
  ): TransactionVIN.HasValues = {
    TransactionVIN.HasValues(txid, voutIndex, value, addresses, pubKeyScript)
  }

  def withValues(
      value: BigDecimal,
      address: Address,
      pubKeyScript: HexString
  ): TransactionVIN.HasValues = {
    TransactionVIN.HasValues(
      txid,
      voutIndex,
      value,
      List(address),
      pubKeyScript
    )
  }
}

object TransactionVIN {

  final case class Raw(
      override val txid: TransactionId,
      override val voutIndex: Int
  ) extends TransactionVIN
  final case class HasValues(
      override val txid: TransactionId,
      override val voutIndex: Int,
      value: BigDecimal,
      addresses: List[Address],
      pubKeyScript: HexString
  ) extends TransactionVIN

  implicit val reads: Reads[TransactionVIN] = {
    val builder = (__ \ 'txid).read[TransactionId] and
      (__ \ 'vout).read[Int]

    builder.apply { (txid, index) =>
      Raw(txid, index)
    }
  }
}
