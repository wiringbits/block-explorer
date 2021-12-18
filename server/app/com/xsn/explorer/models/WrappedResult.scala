package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

import scala.annotation.nowarn

case class WrappedResult[+T](data: T)

object WrappedResult {

  @nowarn implicit def writes[T: Writes]: Writes[WrappedResult[T]] =
    Json.writes[WrappedResult[T]]
}
