package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.data.async.BalanceFutureDataHandler
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.base.{OrderingQuery, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.parsers.BalanceOrderingParser
import com.xsn.explorer.services.validators.PaginatedQueryValidator

import scala.concurrent.ExecutionContext

class BalanceService @Inject() (
    paginatedQueryValidator: PaginatedQueryValidator,
    balanceOrderingParser: BalanceOrderingParser,
    balanceFutureDataHandler: BalanceFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def get(paginatedQuery: PaginatedQuery, orderingQuery: OrderingQuery): FutureApplicationResult[PaginatedResult[Balance]] = {
    val result = for {
      validatedQuery <- paginatedQueryValidator.validate(paginatedQuery).toFutureOr
      ordering <- balanceOrderingParser.from(orderingQuery).toFutureOr
      balances <- balanceFutureDataHandler.get(validatedQuery, ordering).toFutureOr
    } yield balances

    result.toFuture
  }
}
