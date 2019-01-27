package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models._

sealed trait BlockError

case object BlockhashFormatError extends BlockError with InputValidationError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.block.format")
    val error = FieldValidationError("blockhash", message)
    List(error)
  }
}

case object BlockNotFoundError extends BlockError with InputValidationError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.block.notFound")
    val error = FieldValidationError("blockhash", message)
    List(error)
  }
}

case object BlockRewardsNotFoundError extends BlockError with NotFoundError {
  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.block.notFound")
    val error = FieldValidationError("blockhash", message)
    List(error)
  }
}

case object BlockUnknownError extends BlockError with ServerError {
  val id = ErrorId.create
  override def cause: Option[Throwable] = None
  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = List.empty
}
