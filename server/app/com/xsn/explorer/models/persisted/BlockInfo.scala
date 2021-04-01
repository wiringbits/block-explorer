package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.values._
import play.api.libs.json.{Json, Writes}

case class BlockInfo(
    hash: Blockhash,
    previousBlockhash: Option[Blockhash],
    nextBlockhash: Option[Blockhash],
    merkleRoot: Blockhash,
    height: Height,
    time: Long,
    difficulty: BigDecimal,
    transactions: Int,
    tposContract: Option[TransactionId],
    medianTime: Long
)

object BlockInfoCodec {

  val completeWrites: Writes[BlockInfo] = (obj: BlockInfo) => {
    Json.obj(
      "hash" -> obj.hash,
      "previousBlockhash" -> obj.previousBlockhash,
      "nextBlockhash" -> obj.nextBlockhash,
      "merkleRoot" -> obj.merkleRoot,
      "height" -> obj.height,
      "time" -> obj.time,
      "difficulty" -> obj.difficulty,
      "transactions" -> obj.transactions,
      "tposContract" -> obj.tposContract,
      "medianTime" -> obj.medianTime
    )
  }
}
