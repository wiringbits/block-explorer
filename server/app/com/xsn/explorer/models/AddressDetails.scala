package com.xsn.explorer.models

import com.xsn.explorer.models.rpc.AddressBalance
import play.api.libs.json._

case class AddressDetails(balance: AddressBalance, transactionCount: Int)

object AddressDetails {

  implicit val writes: Writes[AddressDetails] = Writes { obj =>
    val values = Map(
      "balance" -> JsNumber(obj.balance.balance),
      "received" -> JsNumber(obj.balance.received),
      "transactionCount" -> JsNumber(obj.transactionCount))

    JsObject.apply(values)
  }
}
