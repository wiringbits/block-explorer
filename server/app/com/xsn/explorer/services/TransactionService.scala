package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureListOps, FutureOps, OrOps}
import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.{OrderingQuery, PaginatedQuery}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.TransactionFutureDataHandler
import com.xsn.explorer.errors.{AddressFormatError, InvalidRawTransactionError, TransactionFormatError, TransactionNotFoundError}
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.TransactionVIN
import com.xsn.explorer.parsers.TransactionOrderingParser
import org.scalactic.{Bad, Good, One, Or}
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class TransactionService @Inject() (
    paginatedQueryValidator: PaginatedQueryValidator,
    transactionOrderingParser: TransactionOrderingParser,
    xsnService: XSNService,
    transactionFutureDataHandler: TransactionFutureDataHandler)(
    implicit ec: ExecutionContext) {

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

  def getTransactions(
      addressString: String,
      paginatedQuery: PaginatedQuery,
      orderingQuery: OrderingQuery): FuturePaginatedResult[TransactionWithValues] = {

    val result = for {
      address <- {
        val maybe = Address.from(addressString)
        Or.from(maybe, One(AddressFormatError)).toFutureOr
      }

      paginatedQuery <- paginatedQueryValidator.validate(paginatedQuery, 100).toFutureOr
      ordering <- transactionOrderingParser.from(orderingQuery).toFutureOr
      transactions <- transactionFutureDataHandler.getBy(address, paginatedQuery, ordering).toFutureOr
    } yield transactions

    result.toFuture
  }

  def sendRawTransaction(hexString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      hex <- Or.from(HexString.from(hexString), One(InvalidRawTransactionError)).toFutureOr
      _ <- xsnService.sendRawTransaction(hex).toFutureOr
    } yield JsObject.empty + ("hex" -> JsString(hex.string))

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
