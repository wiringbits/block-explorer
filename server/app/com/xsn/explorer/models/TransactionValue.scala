package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class TransactionValue(address: Address, value: BigDecimal)

object TransactionValue {

  def from(vout: TransactionVOUT): Option[TransactionValue] = {
    val value = vout.value
    val addressMaybe = vout.address

    addressMaybe.map { address =>
      TransactionValue(address, value)
    }
  }

  implicit val writes: Writes[TransactionValue] = Json.writes[TransactionValue]
}
