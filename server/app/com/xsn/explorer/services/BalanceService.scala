package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.BalanceFutureDataHandler
import com.xsn.explorer.models.WrappedResult
import com.xsn.explorer.models.persisted.Balance
import com.xsn.explorer.parsers.BalanceOrderingParser
import com.xsn.explorer.services.validators._
import javax.inject.Inject

import scala.concurrent.ExecutionContext

class BalanceService @Inject()(
    paginatedQueryValidator: PaginatedQueryValidator,
    balanceOrderingParser: BalanceOrderingParser,
    addressValidator: AddressValidator,
    balanceFutureDataHandler: BalanceFutureDataHandler
)(implicit ec: ExecutionContext) {

  def getHighest(
      limit: Limit,
      lastSeenAddressString: Option[String]
  ): FutureApplicationResult[WrappedResult[List[Balance]]] = {
    val result = for {
      _ <- paginatedQueryValidator.validate(PaginatedQuery(Offset(0), limit), 100).toFutureOr
      lastSeenAddress <- validate(lastSeenAddressString, addressValidator.validate).toFutureOr

      data <- balanceFutureDataHandler.getHighestBalances(limit, lastSeenAddress).toFutureOr
    } yield WrappedResult(data)

    result.toFuture
  }
}
