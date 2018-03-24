package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError}
import play.api.i18n.{Lang, MessagesApi}

sealed trait BlockError

case object BlockhashFormatError extends BlockError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.block.format")
    val error = FieldValidationError("blockhash", message)
    List(error)
  }
}

case object BlockNotFoundError extends BlockError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.block.notFound")
    val error = FieldValidationError("blockhash", message)
    List(error)
  }
}
