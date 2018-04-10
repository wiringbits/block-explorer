package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{PublicError, ServerError}
import play.api.i18n.{Lang, MessagesApi}

sealed trait BalanceError

case object BalanceUnknownError extends BalanceError with ServerError {

  override def cause: Option[Throwable] = None

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = List.empty
}
