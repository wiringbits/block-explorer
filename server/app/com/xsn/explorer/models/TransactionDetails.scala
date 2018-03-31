package com.xsn.explorer.models

import com.xsn.explorer.models.rpc.Transaction
import play.api.libs.json.{Json, Writes}

case class TransactionDetails(
    id: TransactionId,
    size: Size,
    blockhash: Blockhash,
    time: Long,
    blocktime: Long,
    confirmations: Confirmations,
    input: List[TransactionValue],
    output: List[TransactionValue]) {

  lazy val fee: BigDecimal = {
    val vin = input.map(_.value).sum
    val vout = output.map(_.value).sum
    (vin - vout) max 0
  }
}

object TransactionDetails {

  def from(tx: Transaction, input: List[TransactionValue]): TransactionDetails = {
    TransactionDetails
        .from(tx)
        .copy(input = input)
  }

  def from(tx: Transaction): TransactionDetails = {
    val output = tx.vout.flatMap(TransactionValue.from)

    TransactionDetails(tx.id, tx.size, tx.blockhash, tx.time, tx.blocktime, tx.confirmations, List.empty, output)
  }

  implicit val writes: Writes[TransactionDetails] = Json.writes[TransactionDetails]
}
