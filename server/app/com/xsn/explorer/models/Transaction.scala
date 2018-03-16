package com.xsn.explorer.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Transaction(
    id: TransactionId,
    size: Size,
    blockhash: Blockhash,
    time: Long,
    blocktime: Long,
    confirmations: Confirmations,
    vin: Option[TransactionVIN],
    vout: List[TransactionVOUT],
)

object Transaction {

  implicit val reads: Reads[Transaction] = {
    val builder = (__ \ 'txid).read[TransactionId] and
        (__ \ 'size).read[Size] and
        (__ \ 'blockhash).read[Blockhash] and
        (__ \ 'time).read[Long] and
        (__ \ 'blocktime).read[Long] and
        (__ \ 'confirmations).read[Confirmations] and
        (__ \ 'vout).read[List[TransactionVOUT]] and
        (__ \ 'vin).readNullable[List[JsValue]]
            .map(_ getOrElse List.empty)
            .map { list => list.flatMap(_.asOpt[TransactionVIN]) }
            .map(_.headOption)

    builder.apply { (id, size, blockHash, time, blockTime, confirmations, vout, vin) =>
      Transaction(id, size, blockHash, time, blockTime, confirmations, vin, vout)
    }
  }
}
