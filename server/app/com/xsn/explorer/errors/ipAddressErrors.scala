package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError}
import play.api.i18n.{Lang, MessagesApi}

trait IPAddressError

case object IPAddressFormatError extends IPAddressError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.ipAddress.invalid")
    val error = FieldValidationError("ip", message)
    List(error)
  }
}
