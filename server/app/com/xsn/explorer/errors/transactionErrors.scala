package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models._

sealed trait TransactionError

case object TransactionFormatError extends TransactionError with InputValidationError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.transaction.format")
    val error = FieldValidationError("transactionId", message)
    List(error)
  }
}

case object TransactionNotFoundError extends TransactionError with InputValidationError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.transaction.notFound")
    val error = FieldValidationError("transactionId", message)
    List(error)
  }
}

case object TransactionUnknownError extends TransactionError with ServerError {

  val id = ErrorId.create

  override def cause: Option[Throwable] = None

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = List.empty
}

case object InvalidRawTransactionError extends TransactionError with InputValidationError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.rawTransaction.invalid")
    val error = FieldValidationError("hex", message)
    List(error)
  }
}

case object RawTransactionAlreadyExistsError extends TransactionError with ConflictError {

  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val message = i18nService.render("error.rawTransaction.repeated")
    val error = FieldValidationError("hex", message)
    List(error)
  }
}
