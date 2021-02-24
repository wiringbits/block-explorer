package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class WrappedResult[+T](data: T)

object WrappedResult {

  implicit def writes[T: Writes]: Writes[WrappedResult[T]] =
    Json.writes[WrappedResult[T]]
}
