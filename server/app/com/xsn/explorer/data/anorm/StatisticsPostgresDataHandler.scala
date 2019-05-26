package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.StatisticsBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.StatisticsPostgresDAO
import com.xsn.explorer.models.Statistics
import org.scalactic.Good
import play.api.db.Database

class StatisticsPostgresDataHandler @Inject()(override val database: Database, statisticsDAO: StatisticsPostgresDAO)
    extends StatisticsBlockingDataHandler
    with AnormPostgresDataHandler {

  override def getStatistics(): ApplicationResult[Statistics] = withConnection { implicit conn =>
    val result = statisticsDAO.getStatistics
    Good(result)
  }
}
