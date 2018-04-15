package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError}
import play.api.i18n.{Lang, MessagesApi}

sealed trait OrderingError

case object UnknownOrderingFieldError extends OrderingError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.ordering.unknownField")
    val error = FieldValidationError("orderBy", message)

    List(error)
  }
}

case object InvalidOrderingConditionError extends OrderingError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.ordering.unknownCondition")
    val error = FieldValidationError("orderBy", message)

    List(error)
  }
}

case object InvalidOrderingError extends OrderingError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.ordering.invalid")
    val error = FieldValidationError("orderBy", message)

    List(error)
  }
}
