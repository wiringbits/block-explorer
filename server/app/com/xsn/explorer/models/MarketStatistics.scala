package com.xsn.explorer.models

import com.xsn.explorer.services.Currency
import play.api.libs.json.{Json, Writes}

import scala.math.BigDecimal.RoundingMode

case class MarketStatistics(prices: Map[Currency, BigDecimal], marketInformation: MarketInformation)

object MarketStatistics {
  implicit val writes: Writes[MarketStatistics] = (marketStatistics: MarketStatistics) => {
    val values = marketStatistics.prices.map {
      case (currency, price) => currency.entryName.toLowerCase -> price
    }
    .updated("volume", marketStatistics.marketInformation.volume.setScale(8, RoundingMode.HALF_UP))
    .updated("marketcap", marketStatistics.marketInformation.marketcap.setScale(8, RoundingMode.HALF_UP))

    Json.toJson(values)
  }
}
