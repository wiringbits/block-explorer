package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class TransactionDetails(
    id: TransactionId,
    size: Int,
    blockhash: Blockhash,
    time: Long,
    blocktime: Long,
    confirmations: Int,
    input: Option[TransactionValue],
    output: List[TransactionValue]) {

  lazy val fee: BigDecimal = {
    val vin = input.map(_.value).getOrElse(BigDecimal(0))
    val vout = output.map(_.value).sum
    (vin - vout) max 0
  }
}

object TransactionDetails {

  def from(tx: Transaction, previous: Transaction): TransactionDetails = {
    val input = tx.vin.flatMap { vin =>
      val voutMaybe = previous.vout.find(_.n == vin.voutIndex)

      voutMaybe.flatMap(TransactionValue.from)
    }

    TransactionDetails
        .from(tx)
        .copy(input = input)
  }

  def from(tx: Transaction): TransactionDetails = {
    val output = tx.vout.flatMap(TransactionValue.from)

    TransactionDetails(tx.id, tx.size, tx.blockhash, tx.time, tx.blocktime, tx.confirmations, None, output)
  }

  implicit val writes: Writes[TransactionDetails] = Json.writes[TransactionDetails]
}
