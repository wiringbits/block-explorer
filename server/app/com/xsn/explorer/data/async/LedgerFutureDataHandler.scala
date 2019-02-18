package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{LedgerBlockingDataHandler, LedgerDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.persisted.{Block, Transaction}
import javax.inject.Inject

import scala.concurrent.Future

class LedgerFutureDataHandler @Inject() (
    blockingDataHandler: LedgerBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext)
    extends LedgerDataHandler[FutureApplicationResult] {

  override def push(block: Block, transactions: List[Transaction.HasIO]): FutureApplicationResult[Unit] = Future {
    blockingDataHandler.push(block, transactions)
  }

  override def pop(): FutureApplicationResult[Block] = Future {
    blockingDataHandler.pop()
  }
}
