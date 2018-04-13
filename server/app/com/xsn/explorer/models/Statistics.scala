package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class Statistics(
    blocks: Int,
    transactions: Int,
    totalSupply: BigDecimal,
    circulatingSupply: BigDecimal)

object Statistics {

  implicit val writes: Writes[Statistics] = Json.writes[Statistics]
}
