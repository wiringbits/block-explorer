package com.xsn.explorer.data.anorm

import java.time.Instant

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.StatisticsBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.{StatisticsPostgresDAO, TPoSContractDAO}
import com.xsn.explorer.models.values.Address
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}
import javax.inject.Inject
import org.scalactic.Good
import play.api.db.Database

class StatisticsPostgresDataHandler @Inject() (
    override val database: Database,
    statisticsDAO: StatisticsPostgresDAO,
    tposContractDAO: TPoSContractDAO
) extends StatisticsBlockingDataHandler
    with AnormPostgresDataHandler {

  override def getStatistics(): ApplicationResult[Statistics] = withConnection { implicit conn =>
    val result = statisticsDAO.getStatistics
    Good(result)
  }

  override def getRewardsSummary(
      numberOfBlocks: Int
  ): ApplicationResult[BlockRewardsSummary] = withConnection { implicit conn =>
    Good(statisticsDAO.getSummary(numberOfBlocks))
  }

  override def getTPoSMerchantStakingAddresses(
      merchantAddress: Address
  ): ApplicationResult[List[Address]] = withConnection { implicit conn =>
    Good(tposContractDAO.getTPoSMerchantStakingAddresses(merchantAddress))
  }

  override def getRewardedAddressesCount(startDate: Instant): ApplicationResult[Long] =
    withConnection { implicit conn =>
      Good(statisticsDAO.getRewardedAddressesCount(startDate))
    }
}
