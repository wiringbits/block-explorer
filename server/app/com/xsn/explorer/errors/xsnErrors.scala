package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{GenericPublicError, PublicError, ServerError}
import play.api.i18n.{Lang, MessagesApi}

sealed trait XSNServerError extends ServerError {

  override def cause: Option[Throwable] = Option.empty
}

case class XSNMessageError(message: String) extends XSNServerError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val error = GenericPublicError(message)
    List(error)
  }
}

case object XSNUnexpectedResponseError extends XSNServerError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = {
    val message = messagesApi("xsn.server.unexpectedError")
    val error = GenericPublicError(message)
    List(error)
  }
}
