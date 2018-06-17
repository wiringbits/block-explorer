package com.xsn.explorer.data.async

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{TransactionBlockingDataHandler, TransactionDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.{Blockhash, Transaction, TransactionId}

import scala.concurrent.Future

class TransactionFutureDataHandler @Inject() (
    blockingDataHandler: TransactionBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext)
    extends TransactionDataHandler[FutureApplicationResult] {

  override def upsert(transaction: Transaction): FutureApplicationResult[Transaction] = Future {
    blockingDataHandler.upsert(transaction)
  }

  override def delete(transactionId: TransactionId): FutureApplicationResult[Transaction] = Future {
    blockingDataHandler.delete(transactionId)
  }

  override def deleteBy(blockhash: Blockhash): FutureApplicationResult[List[Transaction]] = Future {
    blockingDataHandler.deleteBy(blockhash)
  }
}
