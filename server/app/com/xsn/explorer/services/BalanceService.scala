package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.data.async.BalanceFutureDataHandler
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.base.{PaginatedQuery, PaginatedResult}
import com.xsn.explorer.services.validators.PaginatedQueryValidator

import scala.concurrent.ExecutionContext

class BalanceService @Inject() (
    paginatedQueryValidator: PaginatedQueryValidator,
    balanceFutureDataHandler: BalanceFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def getRichest(query: PaginatedQuery): FutureApplicationResult[PaginatedResult[Balance]] = {
    val result = for {
      validatedQuery <- paginatedQueryValidator.validate(query).toFutureOr
      balances <- balanceFutureDataHandler.getRichest(validatedQuery).toFutureOr
    } yield balances

    result.toFuture
  }
}
