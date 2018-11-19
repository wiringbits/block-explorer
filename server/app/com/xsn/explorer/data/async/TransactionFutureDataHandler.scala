package com.xsn.explorer.data.async

import javax.inject.Inject

import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.{TransactionBlockingDataHandler, TransactionDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField

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

  override def getUnspentOutputs(address: Address): FutureApplicationResult[List[Transaction.Output]] = Future {
    blockingDataHandler.getUnspentOutputs(address)
  }

  override def getByBlockhash(
      blockhash: Blockhash,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): FutureApplicationResult[PaginatedResult[TransactionWithValues]] = Future {

    blockingDataHandler.getByBlockhash(blockhash, paginatedQuery, ordering)
  }

  override def getLatestTransactionBy(addresses: List[Address]): FutureApplicationResult[Map[String, String]] = Future {
    blockingDataHandler.getLatestTransactionBy(addresses)
  }
}
