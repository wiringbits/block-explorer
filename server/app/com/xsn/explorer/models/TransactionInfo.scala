package com.xsn.explorer.models

import com.xsn.explorer.models.persisted.Transaction.{Input, Output}
import com.xsn.explorer.models.values.{Blockhash, Height, Size, TransactionId}
import play.api.libs.json.{JsNull, JsObject, JsString, Json, Writes}

case class TransactionInfo(
    id: TransactionId,
    blockhash: Blockhash,
    time: Long,
    size: Size,
    sent: BigDecimal,
    received: BigDecimal,
    height: Height
) {
  def fee: BigDecimal = (sent - received).max(BigDecimal(0))
}

object TransactionInfo {

  implicit val writes: Writes[TransactionInfo] = (t: TransactionInfo) => {
    Json.obj(
      "id" -> t.id,
      "blockhash" -> t.blockhash,
      "time" -> t.time,
      "size" -> t.size,
      "sent" -> t.sent,
      "received" -> t.received,
      "height" -> t.height,
      "fee" -> t.fee
    )
  }

  case class HasIO(
      transaction: TransactionInfo,
      inputs: List[Input],
      outputs: List[Output]
  ) {

    require(
      outputs.forall(_.txid == transaction.id),
      "There are outputs that having a different txid"
    )

    def id: TransactionId = transaction.id
    def blockhash: Blockhash = transaction.blockhash
    def time: Long = transaction.time
    def size: Size = transaction.size
    def sent: BigDecimal = transaction.sent
    def received: BigDecimal = transaction.received
    def height: Height = transaction.height
    def fee: BigDecimal = transaction.fee
  }

  object HasIO {

    implicit val writes: Writes[HasIO] = (t: HasIO) => {
      val inputs = Json.obj(
        "inputs" -> t.inputs.map { input =>
          Json.obj(
            "txid" -> input.fromTxid,
            "index" -> input.index,
            "value" -> input.value
          )
        }
      )

      val outputs = Json.obj(
        "outputs" -> t.outputs.map { output =>
          val address = output.addresses.headOption
            .map(_.string)
            .map(JsString.apply)
            .getOrElse(JsNull)

          Json.obj(
            "index" -> output.index,
            "value" -> output.value,
            "address" -> address,
            "addresses" -> output.addresses
          )
        }
      )

      Json
        .toJson(t.transaction)
        .as[JsObject]
        .deepMerge(inputs)
        .deepMerge(outputs)
    }
  }
}
