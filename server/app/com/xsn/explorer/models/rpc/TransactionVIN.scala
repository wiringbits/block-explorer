package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.TransactionId
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class TransactionVIN(txid: TransactionId, voutIndex: Int)

object TransactionVIN {

  implicit val reads: Reads[TransactionVIN] = {
    val builder = (__ \ 'txid).read[TransactionId] and (__ \ 'vout).read[Int]

    builder.apply { (txid, index) =>
      TransactionVIN(txid, index)
    }
  }
}
