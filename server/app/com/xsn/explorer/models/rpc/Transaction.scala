package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{
  Blockhash,
  Confirmations,
  Size,
  TransactionId
}
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
      (__ \ 'blocktime).read[Long] and
      (__ \ 'confirmations).read[Confirmations] and
      (__ \ 'vout).read[List[TransactionVOUT]] and
      (__ \ 'vin)
        .readNullable[List[JsValue]]
        .map(_ getOrElse List.empty)
        .map { list =>
          list.flatMap(_.asOpt[TransactionVIN])
        }

    builder.apply {
      (id, size, blockHash, blockTime, confirmations, vout, vin) =>
        Transaction(
          id,
          size,
          blockHash,
          blockTime,
          blockTime,
          confirmations,
          vin,
          vout
        )
    }
  }

  def batchReads(
      blockhash: Blockhash,
      confirmations: Confirmations,
      blocktime: Long
  ): Reads[Transaction[TransactionVIN]] = {
    val builder = (__ \ 'txid).read[TransactionId] and
      (__ \ 'size).read[Size] and
      (__ \ 'vout).read[List[TransactionVOUT]] and
      (__ \ 'vin)
        .readNullable[List[JsValue]]
        .map(_ getOrElse List.empty)
        .map { list =>
          list.flatMap(_.asOpt[TransactionVIN])
        }

    builder.apply { (id, size, vout, vin) =>
      Transaction(
        id,
        size,
        blockhash,
        blocktime,
        blocktime,
        confirmations,
        vin,
        vout
      )
    }
  }
}
