package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{ErrorId, GenericPublicError, PublicError, ServerError}

sealed trait XSNServerError extends ServerError {

  val id = ErrorId.create

  override def cause: Option[Throwable] = Option.empty
}

case class XSNMessageError(message: String) extends XSNServerError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val error = GenericPublicError(message)
    List(error)
  }
}

case object XSNUnexpectedResponseError extends XSNServerError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("xsn.server.unexpectedError")
    val error = GenericPublicError(message)
    List(error)
  }
}

case object XSNWorkQueueDepthExceeded extends XSNServerError {
  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("xsn.server.unexpectedError")
    val error = GenericPublicError(message)
    List(error)
  }
}

case object XSNWarmingUp extends XSNServerError {
  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("xsn.server.unexpectedError")
    val error = GenericPublicError(message)
    List(error)
  }
}
