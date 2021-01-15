package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{ErrorId, PublicError, ServerError}

sealed trait BalanceError

case object BalanceUnknownError extends BalanceError with ServerError {

  val id = ErrorId.create

  override def cause: Option[Throwable] = None

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit
      lang: L
  ): List[PublicError] = List.empty
}
