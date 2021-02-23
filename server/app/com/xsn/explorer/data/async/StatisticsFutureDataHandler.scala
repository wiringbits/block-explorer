package com.xsn.explorer.data.async

import java.time.Instant
import javax.inject.Inject
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{StatisticsBlockingDataHandler, StatisticsDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.{AddressesReward, BlockRewardsSummary, Statistics}
import com.xsn.explorer.models.values.Address

import scala.concurrent.Future

class StatisticsFutureDataHandler @Inject() (
    blockingDataHandler: StatisticsBlockingDataHandler,
    retryableFutureDataHandler: RetryableDataHandler
)(implicit
    ec: DatabaseExecutionContext
) extends StatisticsDataHandler[FutureApplicationResult] {

  override def getStatistics(): FutureApplicationResult[Statistics] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getStatistics()
      }
    }

  override def getRewardsSummary(
      numberOfBlocks: Int
  ): FutureApplicationResult[BlockRewardsSummary] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getRewardsSummary(numberOfBlocks)
      }
    }

  override def getTPoSMerchantStakingAddresses(
      merchantAddress: Address
  ): FutureApplicationResult[List[Address]] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getTPoSMerchantStakingAddresses(merchantAddress)
      }
    }

  override def getRewardedAddresses(startDate: Instant): FutureApplicationResult[AddressesReward] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getRewardedAddresses(startDate)
      }
    }

  override def getStakingCoins(): FutureApplicationResult[BigDecimal] =
    retryableFutureDataHandler.retrying {
      Future {
        blockingDataHandler.getStakingCoins()
      }
    }
}
