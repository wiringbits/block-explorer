package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError}
import play.api.i18n.{Lang, MessagesApi}

sealed trait PaginatedQueryError

case object PaginatedQueryOffsetError extends PaginatedQueryError with InputValidationError {
  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.paginatedQuery.offset")
    val error = FieldValidationError("offset", message)
    List(error)
  }
}

case class PaginatedQueryLimitError(maxValue: Int) extends PaginatedQueryError with InputValidationError {
  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.paginatedQuery.limit", maxValue)
    val error = FieldValidationError("limit", message)
    List(error)
  }
}
