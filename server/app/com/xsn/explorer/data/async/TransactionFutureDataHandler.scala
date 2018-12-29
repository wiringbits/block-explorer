package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Limit, PaginatedQuery}
import com.xsn.explorer.data.{TransactionBlockingDataHandler, TransactionDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import javax.inject.Inject
import org.scalactic.Every

import scala.concurrent.Future

class TransactionFutureDataHandler @Inject() (
    blockingDataHandler: TransactionBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext)
    extends TransactionDataHandler[FutureApplicationResult] {

  override def getBy(
      address: Address,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): FuturePaginatedResult[TransactionWithValues] = Future {

    blockingDataHandler.getBy(address, paginatedQuery, ordering)
  }

  override def getBy(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition): FutureApplicationResult[List[Transaction]] = Future {

    blockingDataHandler.getBy(address, limit, lastSeenTxid, orderingCondition)
  }

  override def getUnspentOutputs(address: Address): FutureApplicationResult[List[Transaction.Output]] = Future {
    blockingDataHandler.getUnspentOutputs(address)
  }

  override def getByBlockhash(
      blockhash: Blockhash,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): FuturePaginatedResult[TransactionWithValues] = Future {

    blockingDataHandler.getByBlockhash(blockhash, paginatedQuery, ordering)
  }

  override def getLatestTransactionBy(addresses: Every[Address]): FutureApplicationResult[Map[String, String]] = Future {
    blockingDataHandler.getLatestTransactionBy(addresses)
  }
}
