package com.xsn.explorer.models

import com.xsn.explorer.models.rpc.AddressBalance
import play.api.libs.json._

case class AddressDetails(balance: AddressBalance, transactions: List[TransactionId])

object AddressDetails {

  implicit val writes: Writes[AddressDetails] = Writes { obj =>
    val transactions = obj.transactions.map { txid => Json.toJson(txid) }
    val values = Map(
      "balance" -> JsNumber(obj.balance.balance),
      "received" -> JsNumber(obj.balance.received),
      "transactions" -> JsArray(transactions))

    JsObject.apply(values)
  }
}
