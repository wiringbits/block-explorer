package com.xsn.explorer.models

import com.xsn.explorer.models.values.{Blockhash, Confirmations, Size, TransactionId}
import play.api.libs.json.{JsValue, Json, Writes}

case class TransactionDetails(
    id: TransactionId,
    size: Size,
    blockhash: Blockhash,
    time: Long,
    blocktime: Long,
    confirmations: Confirmations,
    input: List[TransactionValue],
    output: List[TransactionValue]
) {

  lazy val fee: BigDecimal = {
    val vin = input.map(_.value).sum
    val vout = output.map(_.value).sum
    (vin - vout) max 0
  }
}

object TransactionDetails {

  def from(tx: rpc.Transaction[rpc.TransactionVIN.HasValues]): TransactionDetails = {
    val input = tx.vin.map { vin =>
      TransactionValue(vin.addresses, vin.value, vin.pubKeyScript)
    }

    val output = tx.vout.flatMap(TransactionValue.from)

    TransactionDetails(tx.id, tx.size, tx.blockhash, tx.time, tx.blocktime, tx.confirmations, input, output)
  }

  implicit val writes: Writes[TransactionDetails] = new Writes[TransactionDetails] {
    override def writes(o: TransactionDetails): JsValue = {
      Json.obj(
        "id" -> o.id,
        "size" -> o.size,
        "blockhash" -> o.blockhash,
        "time" -> o.time,
        "blocktime" -> o.blocktime,
        "confirmations" -> o.confirmations,
        "input" -> o.input,
        "output" -> o.output
      )
    }
  }
}
