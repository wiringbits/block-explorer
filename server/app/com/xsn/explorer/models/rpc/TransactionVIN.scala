package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{Address, TransactionId}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

sealed trait TransactionVIN {
  def txid: TransactionId
  def voutIndex: Int

  def withValues(value: BigDecimal, address: Address): TransactionVIN.HasValues = {
    TransactionVIN.HasValues(txid, voutIndex, value, address)
  }
}

object TransactionVIN {

  final case class Raw(override val txid: TransactionId, override val voutIndex: Int) extends TransactionVIN
  final case class HasValues(
      override val txid: TransactionId,
      override val voutIndex: Int,
      value: BigDecimal,
      address: Address) extends TransactionVIN

  implicit val reads: Reads[TransactionVIN] = {
    val builder = (__ \ 'txid).read[TransactionId] and
        (__ \ 'vout).read[Int] and
        (__ \ 'value).readNullable[BigDecimal] and
        (__ \ 'address).readNullable[Address]

    builder.apply { (txid, index, valueMaybe, addressMaybe) =>
      val maybe = for {
        value <- valueMaybe
        address <- addressMaybe
      } yield HasValues(txid, index, value, address)

      maybe.getOrElse(Raw(txid, index))
    }
  }
}
