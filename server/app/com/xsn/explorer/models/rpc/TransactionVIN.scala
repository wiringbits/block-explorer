package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{Address, TransactionId}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class TransactionVIN(
    txid: TransactionId,
    voutIndex: Int,
    value: Option[BigDecimal],
    address: Option[Address])

object TransactionVIN {

  implicit val reads: Reads[TransactionVIN] = {
    val builder = (__ \ 'txid).read[TransactionId] and
        (__ \ 'vout).read[Int] and
        (__ \ 'value).readNullable[BigDecimal] and
        (__ \ 'address).readNullable[Address]

    builder.apply { (txid, index, value, address) =>
      TransactionVIN(txid, index, value, address)
    }
  }
}
