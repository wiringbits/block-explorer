package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{ApplicationError, PublicError}
import play.api.i18n.{Lang, MessagesApi}

trait LedgerError extends ApplicationError {

  override def toPublicErrorList(messagesApi: MessagesApi)(implicit lang: Lang): List[PublicError] = List.empty
}

case object PreviousBlockMissingError extends LedgerError
case object RepeatedBlockhashError extends LedgerError
case object RepeatedBlockHeightError extends LedgerError
