package com.xsn.explorer.data.async

import javax.inject.Inject
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{StatisticsBlockingDataHandler, StatisticsDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}

import scala.concurrent.Future

class StatisticsFutureDataHandler @Inject()(
    blockingDataHandler: StatisticsBlockingDataHandler,
    retryableFutureDataHandler: RetryableDataHandler
)(
    implicit ec: DatabaseExecutionContext
) extends StatisticsDataHandler[FutureApplicationResult] {

  override def getStatistics(): FutureApplicationResult[Statistics] = retryableFutureDataHandler.retrying {
    Future {
      blockingDataHandler.getStatistics()
    }
  }

  override def getRewardsSummary(numberOfBlocks: Int): FutureApplicationResult[BlockRewardsSummary] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getRewardsSummary(numberOfBlocks)
      }
    }
}
