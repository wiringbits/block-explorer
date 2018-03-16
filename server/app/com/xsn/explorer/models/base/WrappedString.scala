package com.xsn.explorer.models.base

import play.api.libs.json.{JsString, Writes}

trait WrappedString extends Any {
  def string: String
}

object WrappedString {

  implicit val writes: Writes[WrappedString] = {
    Writes[WrappedString] { wrapped => JsString(wrapped.string) }
  }
}
