package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.StatisticsParsers
import com.xsn.explorer.models.Statistics

class StatisticsPostgresDAO {

  import StatisticsPostgresDAO._

  def getStatistics(implicit conn: Connection): Statistics = {
    val result = SQL(
      s"""
        |SELECT (
        |    (SELECT value FROM aggregated_amounts WHERE name = 'available_coins') -
        |    (SELECT COALESCE(SUM(received - spent), 0) FROM balances WHERE address = '$BurnAddress')
        |  ) AS total_supply,
        |  (
        |    (SELECT value FROM aggregated_amounts WHERE name = 'available_coins') -
        |    (SELECT COALESCE(SUM(received - spent), 0) FROM balances WHERE address IN (SELECT address FROM hidden_addresses))
        |  ) AS circulating_supply,
        |  (SELECT n_live_tup FROM pg_stat_all_tables WHERE relname = 'transactions') AS transactions,
        |  (SELECT COALESCE(MAX(height), 0) FROM blocks) AS blocks
      """.stripMargin
    ).as(StatisticsParsers.parseStatistics.single)

    val shiftBy = BigDecimal(6000000)
    val totalSupply = result.totalSupply.map(x => (x - shiftBy) max 0)
    val circulatingSupply = result.circulatingSupply.map(x => (x - shiftBy) max 0)
    result.copy(totalSupply = totalSupply, circulatingSupply = circulatingSupply)
  }
}

object StatisticsPostgresDAO {
  /**
   * We need to exclude the burn address from the total supply.
   */
  val BurnAddress = "XmPe9BHRsmZeThtYF34YYjdnrjmcAUn8bC"
}
