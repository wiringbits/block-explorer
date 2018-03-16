package com.xsn.explorer.models.base

import play.api.libs.json.{JsNumber, Writes}

trait WrappedInt extends Any {
  def int: Int
}

object WrappedInt {

  implicit val writes: Writes[WrappedInt] = {
    Writes[WrappedInt] { wrapped => JsNumber(wrapped.int) }
  }
}
