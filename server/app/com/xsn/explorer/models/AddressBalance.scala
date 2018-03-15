package com.xsn.explorer.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AddressBalance(balance: BigInt, received: BigInt)

object AddressBalance {
  implicit val reads: Reads[AddressBalance] = {
    val builder = (__ \ 'balance).read[BigDecimal] and (__ \ 'received).read[BigDecimal]

    builder.apply { (balance, received) =>
      AddressBalance(balance.toBigInt(), received.toBigInt())
    }
  }

  implicit val writes: Writes[AddressBalance] = Writes { obj =>
    val values = Map(
      "balance" -> JsNumber(BigDecimal(obj.balance)),
      "received" -> JsNumber(BigDecimal(obj.received)))

    JsObject.apply(values)
  }
}
