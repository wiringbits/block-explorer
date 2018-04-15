package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.{TransactionFormatError, TransactionNotFoundError}
import com.xsn.explorer.models.rpc.TransactionVIN
import com.xsn.explorer.models.{TransactionDetails, TransactionId, TransactionValue}
import com.xsn.explorer.util.Extensions.FutureApplicationResultExt
import org.scalactic.{Good, One, Or}

import scala.concurrent.{ExecutionContext, Future}

class TransactionService @Inject() (xsnService: XSNService)(implicit ec: ExecutionContext) {

  def getTransaction(txidString: String): FutureApplicationResult[TransactionDetails] = {
    val result = for {
      txid <- {
        val maybe = TransactionId.from(txidString)
        Or.from(maybe, One(TransactionFormatError)).toFutureOr
      }

      transaction <- xsnService.getTransaction(txid).toFutureOr

      input <- transaction
          .vin
          .map(getTransactionValue)
          .toFutureOr
    } yield TransactionDetails.from(transaction, input)

    result.toFuture
  }

  def getTransactionValue(vin: TransactionVIN): FutureApplicationResult[TransactionValue] = {
    val valueMaybe = for {
      value <- vin.value
      address <- vin.address
    } yield TransactionValue(address, value)

    valueMaybe
        .map(Good(_))
        .map(Future.successful)
        .getOrElse {
          val txid = vin.txid

          val result = for {
            tx <- xsnService.getTransaction(txid).toFutureOr
            r <- {
              val maybe = tx
                  .vout
                  .find(_.n == vin.voutIndex)
                  .flatMap(TransactionValue.from)

              Or.from(maybe, One(TransactionNotFoundError)).toFutureOr
            }
          } yield r

          result.toFuture
        }
  }
}
