package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{FieldValidationError, NotFoundError, PublicError}
import play.api.i18n.{Lang, MessagesApi}

trait MasternodeError

case object MasternodeNotFoundError extends MasternodeError with NotFoundError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("error.masternode.notFound")
    val error = FieldValidationError("ip", message)
    List(error)
  }
}
