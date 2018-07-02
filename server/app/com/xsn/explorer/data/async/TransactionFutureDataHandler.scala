package com.xsn.explorer.data.async

import javax.inject.Inject

import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery}
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
}
