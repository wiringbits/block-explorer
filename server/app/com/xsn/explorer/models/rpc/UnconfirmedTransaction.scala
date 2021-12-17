package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{Size, TransactionId}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UnconfirmedTransaction[VIN <: TransactionVIN](
    id: TransactionId,
    size: Size,
    vin: List[VIN],
    vout: List[TransactionVOUT]
)

object UnconfirmedTransaction {

  implicit val reads: Reads[UnconfirmedTransaction[TransactionVIN]] = {
    val builder = (__ \ Symbol("txid")).read[TransactionId] and
      (__ \ Symbol("size")).read[Size] and
      (__ \ Symbol("vout")).read[List[TransactionVOUT]] and
      (__ \ Symbol("vin"))
        .readNullable[List[JsValue]]
        .map(_ getOrElse List.empty)
        .map { list =>
          list.flatMap(_.asOpt[TransactionVIN])
        }

    builder.apply { (id, size, vout, vin) =>
      UnconfirmedTransaction(id, size, vin, vout)
    }
  }
}
