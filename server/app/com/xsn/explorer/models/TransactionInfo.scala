package com.xsn.explorer.models

import com.xsn.explorer.models.values.{Blockhash, Height, Size, TransactionId}
import play.api.libs.json.{Json, Writes}

case class TransactionInfo(
    id: TransactionId,
    blockhash: Blockhash,
    time: Long,
    size: Size,
    sent: BigDecimal,
    received: BigDecimal,
    height: Height
)

object TransactionInfo {

  implicit val writes: Writes[TransactionInfo] = Json.writes[TransactionInfo]
}
