package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class WrappedResult[+T](data: T)

object WrappedResult {

  implicit def writes[T](implicit writesT: Writes[T]): Writes[WrappedResult[T]] = Json.writes[WrappedResult[T]]
}
