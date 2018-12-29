package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.BalanceFutureDataHandler
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.models.{Address, Balance, WrappedResult}
import com.xsn.explorer.parsers.BalanceOrderingParser
import javax.inject.Inject
import org.scalactic.{Good, One, Or}

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

  def getHighest(limit: Limit, lastSeenAddressString: Option[String]): FutureApplicationResult[WrappedResult[List[Balance]]] = {
    val result = for {
      _ <- paginatedQueryValidator.validate(PaginatedQuery(Offset(0), limit), 100).toFutureOr

      lastSeenAddress <- {
        lastSeenAddressString
            .map(Address.from)
            .map { txid => Or.from(txid, One(AddressFormatError)).map(Option.apply) }
            .getOrElse(Good(Option.empty))
            .toFutureOr
      }

      data <- balanceFutureDataHandler.getHighestBalances(limit, lastSeenAddress).toFutureOr
    } yield WrappedResult(data)

    result.toFuture
  }
}
