package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.TransactionFutureDataHandler
import com.xsn.explorer.errors._
import com.xsn.explorer.models._
import com.xsn.explorer.models.transformers._
import com.xsn.explorer.models.values._
import com.xsn.explorer.parsers.{OrderingConditionParser, TransactionOrderingParser}
import com.xsn.explorer.services.validators.AddressValidator
import javax.inject.Inject
import org.scalactic._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class TransactionService @Inject() (
    paginatedQueryValidator: PaginatedQueryValidator,
    orderingConditionParser: OrderingConditionParser,
    transactionOrderingParser: TransactionOrderingParser,
    addressValidator: AddressValidator,
    transactionFutureDataHandler: TransactionFutureDataHandler)(
    implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val maxTransactionsPerQuery = 100

  def getTransactions(
      addressString: String,
      paginatedQuery: PaginatedQuery,
      orderingQuery: OrderingQuery): FuturePaginatedResult[TransactionWithValues] = {

    val result = for {
      address <- addressValidator.validate(addressString).toFutureOr
      paginatedQuery <- paginatedQueryValidator.validate(paginatedQuery, maxTransactionsPerQuery).toFutureOr
      ordering <- transactionOrderingParser.from(orderingQuery).toFutureOr
      transactions <- transactionFutureDataHandler.getBy(address, paginatedQuery, ordering).toFutureOr
    } yield transactions

    result.toFuture
  }

  def getLightWalletTransactions(
      addressString: String,
      limit: Limit,
      lastSeenTxidString: Option[String],
      orderingConditionString: String): FutureApplicationResult[WrappedResult[List[LightWalletTransaction]]] = {

    val result = for {
      address <- addressValidator.validate(addressString).toFutureOr

      _ <- paginatedQueryValidator.validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery).toFutureOr
      orderingCondition <- orderingConditionParser.parseReuslt(orderingConditionString).toFutureOr

      lastSeenTxid <- {
        lastSeenTxidString
            .map(TransactionId.from)
            .map { txid => Or.from(txid, One(TransactionFormatError)).map(Option.apply) }
            .getOrElse(Good(Option.empty))
            .toFutureOr
      }

      transactions <- transactionFutureDataHandler.getBy(address, limit, lastSeenTxid, orderingCondition).toFutureOr
    } yield {
      val lightTxs = transactions.map(toLightWalletTransaction)

      WrappedResult(lightTxs)
    }

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

  def getByBlockhash(blockhashString: String, limit: Limit, lastSeenTxidString: Option[String]): FutureApplicationResult[WrappedResult[List[TransactionWithValues]]] = {
    val result = for {
      blockhash <- Or.from(Blockhash.from(blockhashString), One(BlockhashFormatError)).toFutureOr
      _ <- paginatedQueryValidator.validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery).toFutureOr

      lastSeenTxid <- {
        lastSeenTxidString
            .map(TransactionId.from)
            .map { txid => Or.from(txid, One(TransactionFormatError)).map(Option.apply) }
            .getOrElse(Good(Option.empty))
            .toFutureOr
      }

      r <- transactionFutureDataHandler.getByBlockhash(blockhash, limit, lastSeenTxid).toFutureOr
    } yield WrappedResult(r)

    result.toFuture
  }

  def getLightWalletTransactionsByBlockhash(
      blockhashString: String,
      limit: Limit,
      lastSeenTxidString: Option[String]): FutureApplicationResult[WrappedResult[List[LightWalletTransaction]]] = {

    val result = for {
      blockhash <- Or.from(Blockhash.from(blockhashString), One(BlockhashFormatError)).toFutureOr
      _ <- paginatedQueryValidator.validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery).toFutureOr

      lastSeenTxid <- {
        lastSeenTxidString
            .map(TransactionId.from)
            .map { txid => Or.from(txid, One(TransactionFormatError)).map(Option.apply) }
            .getOrElse(Good(Option.empty))
            .toFutureOr
      }

      transactions <- transactionFutureDataHandler.getTransactionsWithIOBy(blockhash, limit, lastSeenTxid).toFutureOr
    } yield {
      val lightTxs = transactions.map(toLightWalletTransaction)

      WrappedResult(lightTxs)
    }

    result.toFuture
  }
}
