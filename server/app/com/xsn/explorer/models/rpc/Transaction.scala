package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{Blockhash, Confirmations, Size, TransactionId}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Transaction[VIN <: TransactionVIN](
    id: TransactionId,
    size: Size,
    blockhash: Blockhash,
    time: Long,
    blocktime: Long,
    confirmations: Confirmations,
    vin: List[VIN],
    vout: List[TransactionVOUT]
)

object Transaction {

  implicit val reads: Reads[Transaction[TransactionVIN]] = {
    val builder = (__ \ 'txid).read[TransactionId] and
      (__ \ 'size).read[Size] and
      (__ \ 'blockhash).read[Blockhash] and
      (__ \ 'time).readNullable[Long] and
      (__ \ 'blocktime).readNullable[Long] and
      (__ \ 'confirmations).read[Confirmations] and
      (__ \ 'vout).read[List[TransactionVOUT]] and
      (__ \ 'vin)
        .readNullable[List[JsValue]]
        .map(_ getOrElse List.empty)
        .map { list =>
          list.flatMap(_.asOpt[TransactionVIN])
        }

    // TODO: Enfore blocktime and time fields when https://github.com/X9Developers/XSN/issues/72 is fixed.
    builder.apply { (id, size, blockHash, time, blockTime, confirmations, vout, vin) =>
      Transaction(id, size, blockHash, time.getOrElse(0), blockTime.getOrElse(0), confirmations, vin, vout)
    }
  }

  def batchReads(
      blockhash: Blockhash,
      confirmations: Confirmations,
      blocktime: Long
  ): Reads[Transaction[TransactionVIN]] = {
    val builder = (__ \ 'txid).read[TransactionId] and
      (__ \ 'size).read[Size] and
      (__ \ 'time).readNullable[Long] and
      (__ \ 'vout).read[List[TransactionVOUT]] and
      (__ \ 'vin)
        .readNullable[List[JsValue]]
        .map(_ getOrElse List.empty)
        .map { list =>
          list.flatMap(_.asOpt[TransactionVIN])
        }

    builder.apply { (id, size, time, vout, vin) =>
      Transaction(id, size, blockhash, time.getOrElse(0), blocktime, confirmations, vin, vout)
    }
  }
}
