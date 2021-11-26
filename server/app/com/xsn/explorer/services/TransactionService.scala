package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.TransactionFutureDataHandler
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.transformers._
import com.xsn.explorer.parsers.OrderingConditionParser
import com.xsn.explorer.services.validators._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TransactionService @Inject() (
    paginatedQueryValidator: PaginatedQueryValidator,
    orderingConditionParser: OrderingConditionParser,
    addressValidator: AddressValidator,
    transactionIdValidator: TransactionIdValidator,
    blockhashValidator: BlockhashValidator,
    transactionFutureDataHandler: TransactionFutureDataHandler
)(implicit ec: ExecutionContext) {

  private val maxTransactionsPerQuery = 100

  def getLightWalletTransactions(
      addressString: String,
      limit: Limit,
      lastSeenTxidString: Option[String],
      orderingConditionString: String
  ): FutureApplicationResult[WrappedResult[List[TransactionInfo.HasIO]]] = {

    val result = for {
      address <- addressValidator.validate(addressString).toFutureOr

      _ <- paginatedQueryValidator
        .validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery)
        .toFutureOr
      orderingCondition <- orderingConditionParser
        .parseReuslt(orderingConditionString)
        .toFutureOr

      lastSeenTxid <- validate(
        lastSeenTxidString,
        transactionIdValidator.validate
      ).toFutureOr
      transactions <- transactionFutureDataHandler
        .getBy(address, limit, lastSeenTxid, orderingCondition)
        .toFutureOr
    } yield WrappedResult(transactions)

    result.toFuture
  }

  def getByAddress(
      addressString: String,
      limit: Limit,
      lastSeenTxidString: Option[String],
      orderingConditionString: String
  ): FutureApplicationResult[WrappedResult[List[TransactionInfo]]] = {

    val result = for {
      address <- addressValidator.validate(addressString).toFutureOr

      _ <- paginatedQueryValidator
        .validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery)
        .toFutureOr
      orderingCondition <- orderingConditionParser
        .parseReuslt(orderingConditionString)
        .toFutureOr

      lastSeenTxid <- validate(
        lastSeenTxidString,
        transactionIdValidator.validate
      ).toFutureOr
      transactions <- transactionFutureDataHandler
        .getByAddress(address, limit, lastSeenTxid, orderingCondition)
        .toFutureOr
    } yield WrappedResult(transactions)

    result.toFuture
  }

  def get(
      limit: Limit,
      lastSeenTxidString: Option[String],
      orderingConditionString: String,
      includeZeroValueTransactions: Boolean
  ): FutureApplicationResult[WrappedResult[List[TransactionInfo]]] = {
    val result = for {
      _ <- paginatedQueryValidator
        .validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery)
        .toFutureOr

      lastSeenTxid <- validate(
        lastSeenTxidString,
        transactionIdValidator.validate
      ).toFutureOr
      orderingCondition <- orderingConditionParser
        .parseReuslt(orderingConditionString)
        .toFutureOr

      r <- transactionFutureDataHandler
        .get(
          limit,
          lastSeenTxid,
          orderingCondition,
          includeZeroValueTransactions
        )
        .toFutureOr
    } yield WrappedResult(r)

    result.toFuture
  }

  def getByBlockhash(
      blockhashString: String,
      limit: Limit,
      lastSeenTxidString: Option[String]
  ): FutureApplicationResult[WrappedResult[List[TransactionWithValues]]] = {
    val result = for {
      blockhash <- blockhashValidator.validate(blockhashString).toFutureOr
      _ <- paginatedQueryValidator
        .validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery)
        .toFutureOr

      lastSeenTxid <- validate(
        lastSeenTxidString,
        transactionIdValidator.validate
      ).toFutureOr
      r <- transactionFutureDataHandler
        .getByBlockhash(blockhash, limit, lastSeenTxid)
        .toFutureOr
    } yield WrappedResult(r)

    result.toFuture
  }

  def getLightWalletTransactionsByBlockhash(
      blockhashString: String,
      limit: Limit,
      lastSeenTxidString: Option[String]
  ): FutureApplicationResult[WrappedResult[List[LightWalletTransaction]]] = {

    val result = for {
      blockhash <- blockhashValidator.validate(blockhashString).toFutureOr
      _ <- paginatedQueryValidator
        .validate(PaginatedQuery(Offset(0), limit), maxTransactionsPerQuery)
        .toFutureOr

      lastSeenTxid <- validate(
        lastSeenTxidString,
        transactionIdValidator.validate
      ).toFutureOr
      transactions <- transactionFutureDataHandler
        .getTransactionsWithIOBy(blockhash, limit, lastSeenTxid)
        .toFutureOr
    } yield {
      val lightTxs = transactions.map(toLightWalletTransaction)

      WrappedResult(lightTxs)
    }

    result.toFuture
  }

  def getSpendingTransaction(txid: String, outputIndex: Int): FutureApplicationResult[Option[Transaction]] = {
    val result = for {
      transactionId <- transactionIdValidator.validate(txid).toFutureOr
      spendingTransaction <- transactionFutureDataHandler.getSpendingTransaction(transactionId, outputIndex).toFutureOr
    } yield spendingTransaction

    result.toFuture
  }
}
