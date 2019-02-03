package com.xsn.explorer.models

import play.api.libs.json._

case class StatisticsDetails(statistics: Statistics, masternodes: Option[Int], difficulty: Option[Float])

object StatisticsDetails {

  implicit val writes: Writes[StatisticsDetails] = Writes { obj =>
    val values = Map(
      "blocks" -> JsNumber(obj.statistics.blocks),
      "transactions" -> JsNumber(obj.statistics.transactions))

    val extras = List(
      "totalSupply" -> obj.statistics.totalSupply.map(JsNumber.apply),
      "circulatingSupply" -> obj.statistics.circulatingSupply.map(JsNumber.apply),
      "masternodes" -> obj.masternodes.map(c => JsNumber.apply(c)),
      "difficulty" -> obj.difficulty.map(c => JsNumber.apply(c))
    ).flatMap { case (key, maybe) =>
      maybe.map(key -> _)
    }

    val result = extras.foldLeft(values) { case (acc, value) =>
      acc + value
    }

    JsObject.apply(result)
  }
}
