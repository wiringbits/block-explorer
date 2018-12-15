package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError}

trait IPAddressError

case object IPAddressFormatError extends IPAddressError with InputValidationError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.ipAddress.invalid")
    val error = FieldValidationError("ip", message)
    List(error)
  }
}
