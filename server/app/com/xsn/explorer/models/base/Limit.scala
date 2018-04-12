package com.xsn.explorer.models.base

import play.api.libs.json.{JsNumber, Writes}

case class Limit(int: Int) extends AnyVal

object Limit {

  implicit val writes: Writes[Limit] = Writes[Limit] { limit => JsNumber(limit.int) }
}
