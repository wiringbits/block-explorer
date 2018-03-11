package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.TransactionFormatError
import com.xsn.explorer.models.TransactionId
import org.scalactic.{One, Or}
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

class TransactionService @Inject() (xsnService: XSNService)(implicit ec: ExecutionContext) {

  def getTransaction(txidString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      txid <- {
        val maybe = TransactionId.from(txidString)
        Or.from(maybe, One(TransactionFormatError)).toFutureOr
      }

      transaction <- xsnService.getTransaction(txid).toFutureOr
    } yield transaction

    result.toFuture
  }
}
