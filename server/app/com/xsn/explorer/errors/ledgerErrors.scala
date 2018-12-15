package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{ApplicationError, PublicError}

trait LedgerError extends ApplicationError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = List.empty
}

case object PreviousBlockMissingError extends LedgerError
case object RepeatedBlockhashError extends LedgerError
case object RepeatedBlockHeightError extends LedgerError
