package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureListOps, FutureOps, OrOps}
import com.xsn.explorer.errors.{TransactionFormatError, TransactionNotFoundError}
import com.xsn.explorer.models.rpc.TransactionVIN
import com.xsn.explorer.models.{Transaction, TransactionDetails, TransactionId, TransactionValue}
import org.scalactic.{Bad, Good, One, Or}
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

class TransactionService @Inject() (xsnService: XSNService)(implicit ec: ExecutionContext) {

  def getRawTransaction(txidString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      txid <- {
        val maybe = TransactionId.from(txidString)
        Or.from(maybe, One(TransactionFormatError)).toFutureOr
      }

      transaction <- xsnService.getRawTransaction(txid).toFutureOr
    } yield transaction

    result.toFuture
  }

  def getTransactionDetails(txidString: String): FutureApplicationResult[TransactionDetails] = {
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

  def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction] = {
    val result = for {
      tx <- xsnService.getTransaction(txid).toFutureOr
      transactionVIN <- tx.vin.map { vin =>
        getTransactionValue(vin)
            .map {
              case Good(transactionValue) =>
                val newVIN = vin.copy(address = Some(transactionValue.address), value = Some(transactionValue.value))
                Good(newVIN)

              case Bad(_) => Good(vin)
            }
      }.toFutureOr

      rpcTransaction = tx.copy(vin = transactionVIN)
    } yield Transaction.fromRPC(rpcTransaction)

    result.toFuture
  }

  private def getTransactionValue(vin: TransactionVIN): FutureApplicationResult[TransactionValue] = {
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
