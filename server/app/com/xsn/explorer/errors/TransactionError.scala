package com.xsn.explorer.errors

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models._
import com.xsn.explorer.models.values.{Blockhash, TransactionId}

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
}
