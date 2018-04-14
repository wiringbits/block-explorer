package com.xsn.explorer.models

import play.api.libs.json._

case class StatisticsDetails(statistics: Statistics, masternodes: Option[Int])

object StatisticsDetails {

  implicit val writes: Writes[StatisticsDetails] = Writes { obj =>
    val values = Map(
      "blocks" -> JsNumber(obj.statistics.blocks),
      "transactions" -> JsNumber(obj.statistics.transactions),
      "totalSupply" -> JsNumber(obj.statistics.totalSupply),
      "circulatingSupply" -> JsNumber(obj.statistics.circulatingSupply))

    val result = obj.masternodes
        .map { count =>
          values + ("masternodes" -> JsNumber(count))
        }
        .getOrElse {
          values
        }

    JsObject.apply(result)
  }
}
