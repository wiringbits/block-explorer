package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class LightWalletTransaction(
    id: TransactionId,
    blockhash: Blockhash,
    size: Size,
    time: Long,
    inputs: List[LightWalletTransaction.Input],
    outputs: List[LightWalletTransaction.Output])

object LightWalletTransaction {

  case class Input(txid: TransactionId, index: Int, value: BigDecimal)
  case class Output(index: Int, value: BigDecimal)

  implicit val inputWrites: Writes[Input] = Json.writes[Input]
  implicit val outputWrites: Writes[Output] = Json.writes[Output]
  implicit val writes: Writes[LightWalletTransaction] = Json.writes[LightWalletTransaction]
}
