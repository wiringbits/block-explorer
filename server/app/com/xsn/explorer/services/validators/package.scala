package com.xsn.explorer.services

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ApplicationError
import org.scalactic.{Good, One, Or}

package object validators {

  def optional[T](string: String, error: ApplicationError)(
      builder: String => Option[T]
  ): ApplicationResult[T] = {
    val maybe = builder(string)
    Or.from(maybe, One(error))
  }

  def validate[T](
      maybe: Option[String],
      validator: String => ApplicationResult[T]
  ): ApplicationResult[Option[T]] = {
    maybe
      .map { string =>
        validator(string).map(Option.apply)
      }
      .getOrElse(Good(None))
  }
}
