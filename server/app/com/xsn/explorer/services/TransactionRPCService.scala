package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.models.TransactionDetails
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.validators.TransactionIdValidator
import javax.inject.Inject
import org.scalactic.{One, Or}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, JsValue, Json}

import scala.concurrent.ExecutionContext

class TransactionRPCService @Inject() (
    transactionIdValidator: TransactionIdValidator,
    transactionCollectorService: TransactionCollectorService,
    xsnService: XSNService)(
    implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getRawTransaction(txidString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      txid <- transactionIdValidator.validate(txidString).toFutureOr
      transaction <- xsnService.getRawTransaction(txid).toFutureOr
    } yield transaction

    result.toFuture
  }

  def getTransactionDetails(txidString: String): FutureApplicationResult[TransactionDetails] = {
    val result = for {
      txid <- transactionIdValidator.validate(txidString).toFutureOr
      transaction <- xsnService.getTransaction(txid).toFutureOr
      vin <- transactionCollectorService.getRPCTransactionVIN(transaction.vin, List.empty).toFutureOr
    } yield TransactionDetails.from(transaction.copy(vin = vin))

    result.toFuture
  }

  def sendRawTransaction(hexString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      hex <- Or.from(HexString.from(hexString), One(TransactionError.InvalidRawTransaction)).toFutureOr
      txid <- xsnService.sendRawTransaction(hex).toFutureOr
    } yield Json.obj("txid" -> JsString(txid))

    result.toFuture
  }
}
