package com.xsn.explorer.models

import com.xsn.explorer.models.rpc.TransactionVOUT
import com.xsn.explorer.models.values.Address
import play.api.libs.json.{JsNull, JsString, Json, Writes}

case class TransactionValue(addresses: List[Address], value: BigDecimal) {
  def address: Option[Address] = addresses.headOption
}

object TransactionValue {

  def apply(address: Address, value: BigDecimal): TransactionValue = {
    TransactionValue(List(address), value)
  }

  def from(vout: TransactionVOUT): Option[TransactionValue] = {
    for (addresses <- vout.addresses)
      yield TransactionValue(addresses, vout.value)
  }

  implicit val writes: Writes[TransactionValue] = (obj: TransactionValue) => {
    val address = obj.address.map(_.string).map(JsString.apply).getOrElse(JsNull)
    Json.obj(
      "address" -> address, // Keeps compatibility
      "addresses" -> obj.addresses,
      "value" -> obj.value
    )
  }
}
