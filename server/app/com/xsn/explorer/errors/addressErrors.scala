package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{
  FieldValidationError,
  InputValidationError,
  PublicError
}

sealed trait AddressError

case object AddressFormatError extends AddressError with InputValidationError {

  override def toPublicErrorList[L](
      i18nService: I18nService[L]
  )(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.address.format")
    val error = FieldValidationError("address", message)
    List(error)
  }
}
