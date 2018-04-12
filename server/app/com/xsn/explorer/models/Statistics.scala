package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class Statistics(
    height: Height,
    transactions: Int,
    totalSupply: BigDecimal,
    circulatingSupply: BigDecimal)

object Statistics {

  implicit val writes: Writes[Statistics] = Json.writes[Statistics]
}
