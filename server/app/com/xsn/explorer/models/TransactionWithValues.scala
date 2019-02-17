package com.xsn.explorer.models

import com.xsn.explorer.models.values.{Blockhash, Size}
import play.api.libs.json.{Json, Writes}

case class TransactionWithValues(
    id: TransactionId,
    blockhash: Blockhash,
    time: Long,
    size: Size,
    sent: BigDecimal,
    received: BigDecimal)

object TransactionWithValues {

  implicit val writes: Writes[TransactionWithValues] = Json.writes[TransactionWithValues]
}
