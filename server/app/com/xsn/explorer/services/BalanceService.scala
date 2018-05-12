package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.FuturePaginatedResult
import com.alexitc.playsonify.models.{OrderingQuery, PaginatedQuery}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.BalanceFutureDataHandler
import com.xsn.explorer.models.Balance
import com.xsn.explorer.parsers.BalanceOrderingParser

import scala.concurrent.ExecutionContext

class BalanceService @Inject() (
    paginatedQueryValidator: PaginatedQueryValidator,
    balanceOrderingParser: BalanceOrderingParser,
    balanceFutureDataHandler: BalanceFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def get(paginatedQuery: PaginatedQuery, orderingQuery: OrderingQuery): FuturePaginatedResult[Balance] = {
    val result = for {
      validatedQuery <- paginatedQueryValidator.validate(paginatedQuery, 100).toFutureOr
      ordering <- balanceOrderingParser.from(orderingQuery).toFutureOr
      balances <- balanceFutureDataHandler.getNonZeroBalances(validatedQuery, ordering).toFutureOr
    } yield balances

    result.toFuture
  }
}
