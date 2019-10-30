package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{LedgerBlockingDataHandler, LedgerDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.{BlockRewards, TPoSContract}
import com.xsn.explorer.models.persisted.Block
import javax.inject.Inject

import scala.concurrent.Future

class LedgerFutureDataHandler @Inject()(
    blockingDataHandler: LedgerBlockingDataHandler,
    retryableFutureDataHandler: RetryableDataHandler
)(
    implicit ec: DatabaseExecutionContext
) extends LedgerDataHandler[FutureApplicationResult] {

  override def push(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.push(block, tposContracts, filterFactory, rewards)
      }
    }
  }

  override def pop(): FutureApplicationResult[Block] = retryableFutureDataHandler.retrying {
    Future {
      blockingDataHandler.pop()
    }
  }
}
