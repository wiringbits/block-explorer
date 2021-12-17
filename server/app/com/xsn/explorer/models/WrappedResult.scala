package com.xsn.explorer.models

import com.github.ghik.silencer.silent
import play.api.libs.json.{Json, Writes}

case class WrappedResult[+T](data: T)

object WrappedResult {

  @silent implicit def writes[T: Writes]: Writes[WrappedResult[T]] =
    Json.writes[WrappedResult[T]]
}
