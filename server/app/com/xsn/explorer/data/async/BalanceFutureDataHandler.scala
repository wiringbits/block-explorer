package com.xsn.explorer.data.async

import javax.inject.Inject

import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery}
import com.xsn.explorer.data.{BalanceBlockingDataHandler, BalanceDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.fields.BalanceField

import scala.concurrent.Future

class BalanceFutureDataHandler @Inject() (
    blockingDataHandler: BalanceBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext)
    extends BalanceDataHandler[FutureApplicationResult] {

  override def upsert(balance: Balance): FutureApplicationResult[Balance] = Future {
    blockingDataHandler.upsert(balance)
  }

  override def get(query: PaginatedQuery, ordering: FieldOrdering[BalanceField]): FuturePaginatedResult[Balance] = Future {
    blockingDataHandler.get(query, ordering)
  }
}
