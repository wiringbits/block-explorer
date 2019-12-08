package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class Prices(usd: BigDecimal, eur: BigDecimal)

object Prices {

  implicit val writes: Writes[Prices] = Json.writes[Prices]
}
