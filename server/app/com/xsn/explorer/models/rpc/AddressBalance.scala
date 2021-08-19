package com.xsn.explorer.models.rpc

import com.xsn.explorer.util.Extensions.BigDecimalExt
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AddressBalance(balance: BigDecimal, received: BigDecimal)

object AddressBalance {

  /** The RPC server is giving us these values in satoshis, we transform them to
    * BigDecimal to match the format used by the application.
    */
  implicit val reads: Reads[AddressBalance] = {
    val builder =
      (__ \ 'balance).read[BigDecimal] and (__ \ 'received).read[BigDecimal]

    builder.apply { (balance, received) =>
      AddressBalance(balance.fromSatoshis, received.fromSatoshis)
    }
  }

  implicit val writes: Writes[AddressBalance] = Json.writes[AddressBalance]
}
