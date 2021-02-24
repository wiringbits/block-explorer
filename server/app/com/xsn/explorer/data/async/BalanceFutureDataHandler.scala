package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.{
  FutureApplicationResult,
  FuturePaginatedResult
}
import com.alexitc.playsonify.models.ordering.FieldOrdering
import com.alexitc.playsonify.models.pagination
import com.alexitc.playsonify.models.pagination.PaginatedQuery
import com.xsn.explorer.data.{BalanceBlockingDataHandler, BalanceDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.fields.BalanceField
import com.xsn.explorer.models.persisted.Balance
import com.xsn.explorer.models.values.Address
import javax.inject.Inject

import scala.concurrent.Future

class BalanceFutureDataHandler @Inject() (
    blockingDataHandler: BalanceBlockingDataHandler,
    retryableFutureDataHandler: RetryableDataHandler
)(implicit
    ec: DatabaseExecutionContext
) extends BalanceDataHandler[FutureApplicationResult] {

  override def upsert(balance: Balance): FutureApplicationResult[Balance] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.upsert(balance)
      }
    }

  override def get(
      query: PaginatedQuery,
      ordering: FieldOrdering[BalanceField]
  ): FuturePaginatedResult[Balance] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.get(query, ordering)
      }
    }

  override def getBy(address: Address): FutureApplicationResult[Balance] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getBy(address)
      }
    }

  override def getHighestBalances(
      limit: pagination.Limit,
      lastSeenAddress: Option[Address]
  ): FutureApplicationResult[List[Balance]] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getHighestBalances(limit, lastSeenAddress)
      }
    }
}
