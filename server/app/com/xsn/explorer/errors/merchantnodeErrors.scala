package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{
  FieldValidationError,
  NotFoundError,
  PublicError
}

trait MerchantnodeError

case object MerchantnodeNotFoundError
    extends MerchantnodeError
    with NotFoundError {

  override def toPublicErrorList[L](
      i18nService: I18nService[L]
  )(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.merchantnode.notFound")
    val error = FieldValidationError("ip", message)
    List(error)
  }
}
