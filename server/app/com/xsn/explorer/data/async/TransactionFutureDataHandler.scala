package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.data.{TransactionBlockingDataHandler, TransactionDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, Blockhash, TransactionId}
import javax.inject.Inject

import scala.concurrent.Future

class TransactionFutureDataHandler @Inject() (
    blockingDataHandler: TransactionBlockingDataHandler,
    retryableFutureDataHandler: RetryableDataHandler
)(implicit
    ec: DatabaseExecutionContext
) extends TransactionDataHandler[FutureApplicationResult] {

  override def getBy(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): FutureApplicationResult[List[Transaction.HasIO]] =
    retryableFutureDataHandler.retrying {
      Future {

        blockingDataHandler.getBy(
          address,
          limit,
          lastSeenTxid,
          orderingCondition
        )
      }
    }

  override def getByAddress(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): FutureApplicationResult[List[TransactionInfo]] =
    retryableFutureDataHandler.retrying {
      Future {

        blockingDataHandler.getByAddress(
          address,
          limit,
          lastSeenTxid,
          orderingCondition
        )
      }
    }

  override def getUnspentOutputs(
      address: Address
  ): FutureApplicationResult[List[Transaction.Output]] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getUnspentOutputs(address)
      }
    }

  override def getOutput(
      txid: TransactionId,
      index: Int
  ): FutureApplicationResult[Transaction.Output] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getOutput(txid, index)
      }
    }

  override def get(
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition,
      includeZeroTransactions: Boolean
  ): FutureApplicationResult[List[TransactionInfo]] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.get(limit, lastSeenTxid, orderingCondition, includeZeroTransactions)
      }
    }

  override def getByBlockhash(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]
  ): FutureApplicationResult[List[TransactionWithValues]] =
    retryableFutureDataHandler.retrying {
      Future {

        blockingDataHandler.getByBlockhash(blockhash, limit, lastSeenTxid)
      }
    }

  override def getTransactionsWithIOBy(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]
  ): FutureApplicationResult[List[Transaction.HasIO]] =
    retryableFutureDataHandler.retrying {
      Future {

        blockingDataHandler.getTransactionsWithIOBy(
          blockhash,
          limit,
          lastSeenTxid
        )
      }
    }
}
