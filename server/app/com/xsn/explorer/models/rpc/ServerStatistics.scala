package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.Height
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes, __}

case class ServerStatistics(
    height: Height,
    transactions: Int,
    totalSupply: BigDecimal
)

object ServerStatistics {

  implicit val reads: Reads[ServerStatistics] = {
    val builder = (__ \ "height").read[Height] and
      (__ \ "transactions").read[Int] and
      (__ \ "total_amount").read[BigDecimal]

    builder.apply { (height, transactions, totalSupply) =>
      ServerStatistics(height, transactions, totalSupply)
    }
  }

  implicit val writes: Writes[ServerStatistics] = Json.writes[ServerStatistics]
}
