package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureListOps, FutureOps, OrOps}
import com.alexitc.playsonify.core.{FutureApplicationResult, FutureOr}
import com.xsn.explorer.errors.{InvalidRawTransactionError, TransactionNotFoundError, XSNWorkQueueDepthExceeded}
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.rpc.TransactionVIN
import com.xsn.explorer.models.values._
import com.xsn.explorer.models.{TPoSContract, TransactionDetails, TransactionValue}
import com.xsn.explorer.services.validators.TransactionIdValidator
import com.xsn.explorer.util.Extensions.FutureOrExt
import javax.inject.Inject
import org.scalactic.{Bad, Good, One, Or}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TransactionRPCService @Inject() (
    transactionIdValidator: TransactionIdValidator,
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
      vin <- getTransactionVIN(transaction.vin).toFutureOr
    } yield TransactionDetails.from(transaction.copy(vin = vin))

    result.toFuture
  }

  def getTransaction(txid: TransactionId): FutureApplicationResult[(Transaction.HasIO, Option[TPoSContract])] = {
    val result = for {
      tx <- xsnService.getTransaction(txid).toFutureOr
      transactionVIN <- getTransactionVIN(tx.vin).toFutureOr
      rpcTransaction = tx.copy(vin = transactionVIN)
    } yield Transaction.fromRPC(rpcTransaction)

    result.toFuture
  }

  private def getTransactionVIN(list: List[TransactionVIN]): FutureApplicationResult[List[TransactionVIN.HasValues]] = {
    def getVIN(vin: TransactionVIN) = {
      getTransactionValue(vin)
          .map {
            case Good(transactionValue) =>
              val newVIN = vin.withValues(address = transactionValue.address, value = transactionValue.value)
              Good(newVIN)

            case Bad(e) => Bad(e)
          }
    }

    def loadVINSequentially(pending: List[TransactionVIN]): FutureOr[List[TransactionVIN.HasValues]] = pending match {
      case x :: xs =>
        for {
          tx <- getVIN(x).toFutureOr
          next <- loadVINSequentially(xs)
        } yield tx :: next

      case _ => Future.successful(Good(List.empty)).toFutureOr
    }

    list
        .map(getVIN)
        .toFutureOr
        .toFuture
        .recoverWith {
          case NonFatal(ex) =>
            logger.warn(s"Failed to load VIN, trying sequentially, error = ${ex.getMessage}")
            loadVINSequentially(list).toFuture
        }
  }

  def getTransactions(ids: List[TransactionId]): FutureApplicationResult[(List[Transaction.HasIO], List[TPoSContract])] = {
    def loadTransactionsSlowly(pending: List[TransactionId]): FutureOr[List[(Transaction.HasIO, Option[TPoSContract])]] = pending match {
      case x :: xs =>
        for {
          tx <- getTransaction(x).toFutureOr
          next <- loadTransactionsSlowly(xs)
        } yield tx :: next

      case _ => Future.successful(Good(List.empty)).toFutureOr
    }

    ids
        .map(getTransaction)
        .toFutureOr
        .recoverWith(XSNWorkQueueDepthExceeded) {
          logger.warn("Unable to load transaction due to server overload, loading them slowly")
          loadTransactionsSlowly(ids)
        }
        .toFuture
        .recoverWith {
          case NonFatal(ex) =>
            logger.warn(s"Unable to load transactions due to server error, loading them sequentially, error = ${ex.getMessage}")
            loadTransactionsSlowly(ids).toFuture
        }
        .toFutureOr
        .map { result =>
          val contracts = result.flatMap(_._2)
          val txs = result.map(_._1)
          (txs, contracts)
        }
        .toFuture
  }

  def sendRawTransaction(hexString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      hex <- Or.from(HexString.from(hexString), One(InvalidRawTransactionError)).toFutureOr
      txid <- xsnService.sendRawTransaction(hex).toFutureOr
    } yield Json.obj("txid" -> JsString(txid))

    result.toFuture
  }

  private def getTransactionValue(vin: TransactionVIN): FutureApplicationResult[TransactionValue] = {
    val valueMaybe = vin match {
      case x: TransactionVIN.HasValues => Some(TransactionValue(x.address, x.value))
      case _ => None
    }

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
