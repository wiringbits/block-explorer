package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError}
import play.api.i18n.{Lang, MessagesApi}

sealed trait AddressError

case object AddressFormatError extends AddressError with InputValidationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.address.format")
    val error = FieldValidationError("address", message)
    List(error)
  }
}
