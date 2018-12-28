package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureListOps, FutureOps, OrOps}
import com.alexitc.playsonify.core.{FutureApplicationResult, FutureOr, FuturePaginatedResult}
import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.TransactionFutureDataHandler
import com.xsn.explorer.errors._
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.TransactionVIN
import com.xsn.explorer.parsers.TransactionOrderingParser
import com.xsn.explorer.util.Extensions.FutureOrExt
import io.scalaland.chimney.dsl._
import javax.inject.Inject
import org.scalactic._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TransactionService @Inject() (
    paginatedQueryValidator: PaginatedQueryValidator,
    transactionOrderingParser: TransactionOrderingParser,
    xsnService: XSNService,
    transactionFutureDataHandler: TransactionFutureDataHandler)(
    implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val maxTransactionsPerQuery = 100

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
      transactionVIN <- getTransactionVIN(tx.vin).toFutureOr
      rpcTransaction = tx.copy(vin = transactionVIN)
    } yield Transaction.fromRPC(rpcTransaction)

    result.toFuture
  }

  private def getTransactionVIN(list: List[TransactionVIN]): FutureApplicationResult[List[TransactionVIN]] = {
    def getVIN(vin: TransactionVIN) = {
      getTransactionValue(vin)
          .map {
            case Good(transactionValue) =>
              val newVIN = vin.copy(address = Some(transactionValue.address), value = Some(transactionValue.value))
              Good(newVIN)

            case Bad(_) => Good(vin)
          }
    }

    def loadVINSequentially(pending: List[TransactionVIN]): FutureOr[List[TransactionVIN]] = pending match {
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

  def getTransactions(ids: List[TransactionId]): FutureApplicationResult[List[Transaction]] = {
    def loadTransactionsSlowly(pending: List[TransactionId]): FutureOr[List[Transaction]] = pending match {
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

      paginatedQuery <- paginatedQueryValidator.validate(paginatedQuery, maxTransactionsPerQuery).toFutureOr
      ordering <- transactionOrderingParser.from(orderingQuery).toFutureOr
      transactions <- transactionFutureDataHandler.getBy(address, paginatedQuery, ordering).toFutureOr
    } yield transactions

    result.toFuture
  }

  def getLightWalletTransactions(addressString: String, limit: Limit, lastSeenTxidString: Option[String]): FutureApplicationResult[List[LightWalletTransaction]] = {
    def buildData(address: Address, txValues: Transaction) = {
      val result = for {
        plain <- xsnService.getTransaction(txValues.id).toFutureOr
        vin <- getTransactionVIN(plain.vin).toFutureOr
      } yield {
        val inputs = vin
            .collect {
              case TransactionVIN(txid, index, Some(value), Some(a)) if a == address =>
                LightWalletTransaction.Input(txid, index, value)
            }

        val outputs = plain
            .vout
            .filter(_.address contains address)
            .map { _.into[LightWalletTransaction.Output].withFieldRenamed(_.n, _.index).transform }

        txValues
            .into[LightWalletTransaction]
            .withFieldConst(_.inputs, inputs)
            .withFieldConst(_.outputs, outputs)
            .transform
      }

      result.toFuture
    }

    val result = for {
      address <- {
        val maybe = Address.from(addressString)
        Or.from(maybe, One(AddressFormatError)).toFutureOr
      }

      _ <- paginatedQueryValidator.validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery).toFutureOr

      lastSeenTxid <- {
        lastSeenTxidString
            .map(TransactionId.from)
            .map { txid => Or.from(txid, One(TransactionFormatError)).map(Option.apply) }
            .getOrElse(Good(Option.empty))
            .toFutureOr
      }

      transactions <- transactionFutureDataHandler.getLatestBy(address, limit, lastSeenTxid).toFutureOr
      data <- transactions.map { transaction => buildData(address, transaction) }.toFutureOr
    } yield data

    result.toFuture
  }

  def sendRawTransaction(hexString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      hex <- Or.from(HexString.from(hexString), One(InvalidRawTransactionError)).toFutureOr
      _ <- xsnService.sendRawTransaction(hex).toFutureOr
    } yield JsObject.empty + ("hex" -> JsString(hex.string))

    result.toFuture
  }

  def getByBlockhash(blockhashString: String, paginatedQuery: PaginatedQuery, orderingQuery: OrderingQuery): FuturePaginatedResult[TransactionWithValues] = {
    val result = for {
      blockhash <- Or.from(Blockhash.from(blockhashString), One(BlockhashFormatError)).toFutureOr
      validatedQuery <- paginatedQueryValidator.validate(paginatedQuery, maxTransactionsPerQuery).toFutureOr
      order <- transactionOrderingParser.from(orderingQuery).toFutureOr
      r <- transactionFutureDataHandler.getByBlockhash(blockhash, validatedQuery, order).toFutureOr
    } yield r

    result.toFuture
  }

  def getLatestTransactionBy(addresses: Every[Address]): FutureApplicationResult[Map[String, String]] = {
    transactionFutureDataHandler.getLatestTransactionBy(addresses)
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
