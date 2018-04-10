package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError, ServerError}
import play.api.i18n.{Lang, MessagesApi}

sealed trait TransactionError

case object TransactionFormatError extends TransactionError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.transaction.format")
    val error = FieldValidationError("transactionId", message)
    List(error)
  }
}

case object TransactionNotFoundError extends TransactionError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.transaction.notFound")
    val error = FieldValidationError("transactionId", message)
    List(error)
  }
}

case object TransactionUnknownError extends TransactionError with ServerError {

  override def cause: Option[Throwable] = None

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = List.empty
}
