package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{FieldValidationError, NotFoundError, PublicError}

trait MasternodeError

case object MasternodeNotFoundError extends MasternodeError with NotFoundError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.masternode.notFound")
    val error = FieldValidationError("ip", message)
    List(error)
  }
}
