package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models._
import com.xsn.explorer.models.values.{Blockhash, Height, TransactionId}

sealed trait TransactionError

object TransactionError {

  final case object InvalidFormat extends TransactionError with InputValidationError {

    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.transaction.format")
      val error = FieldValidationError("transactionId", message)
      List(error)
    }
  }

  final case class NotFound(txid: TransactionId) extends TransactionError with InputValidationError {

    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.transaction.notFound")
      val error = FieldValidationError("transactionId", message)
      List(error)
    }
  }

  final case class IndexNotFound(height: Height, index: Int) extends TransactionError with InputValidationError {

    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.transaction.notFound")
      val errorHeight = FieldValidationError("height", message)
      val errorIndex = FieldValidationError("index", message)

      List(errorHeight, errorIndex)
    }
  }

  final case class OutputNotFound(txid: TransactionId, index: Int) extends TransactionError with InputValidationError {

    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.transaction.notFound")
      val error = FieldValidationError("transactionId", message)
      List(error)
    }
  }

  final case class CoinbaseNotFound(blockhash: Blockhash) extends TransactionError with InputValidationError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.transaction.notFound")
      val error = FieldValidationError("transactionId", message)
      List(error)
    }
  }

  final case object InvalidRawTransaction extends TransactionError with InputValidationError {

    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.rawTransaction.invalid")
      val error = FieldValidationError("hex", message)
      List(error)
    }
  }

  final case object RawTransactionAlreadyExists extends TransactionError with ConflictError {

    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.rawTransaction.repeated")
      val error = FieldValidationError("hex", message)
      List(error)
    }
  }

  final case object MissingInputs extends TransactionError with ConflictError {

    // This is done this way because previously missing inputs error was not being handled so it ended up as
    // an XSNMessageError("missing inputs") and the wallet team requested us not to change the response
    // of the endpoint in those cases. But at the same time we did not want to keep it as an XSNMessageError
    // since those are sent to sentry.
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val error = GenericPublicError("missing inputs")
      List(error)
    }
  }

  final case object UnconfirmedTransaction extends TransactionError with NotFoundError {
    override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
      val message = i18nService.render("error.transaction.unconfirmed")
      val error = GenericPublicError(message)
      List(error)
    }
  }
}
