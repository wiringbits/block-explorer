package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{LedgerBlockingDataHandler, LedgerDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.persisted.Block
import javax.inject.Inject

import scala.concurrent.Future

class LedgerFutureDataHandler @Inject()(blockingDataHandler: LedgerBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext
) extends LedgerDataHandler[FutureApplicationResult] {

  override def push(block: Block.HasTransactions, tposContracts: List[TPoSContract]): FutureApplicationResult[Unit] =
    Future {
      blockingDataHandler.push(block, tposContracts)
    }

  override def pop(): FutureApplicationResult[Block] = Future {
    blockingDataHandler.pop()
  }
}
