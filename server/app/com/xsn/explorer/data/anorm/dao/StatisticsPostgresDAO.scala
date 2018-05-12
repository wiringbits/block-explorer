package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.StatisticsParsers
import com.xsn.explorer.models.Statistics

class StatisticsPostgresDAO {

  def getStatistics(implicit conn: Connection): Statistics = {
    SQL(
      """
        |SELECT
        |  (SELECT SUM(received - spent) FROM balances) AS total_supply,
        |  (
        |    SELECT SUM(received - spent) FROM balances
        |    WHERE address NOT IN (SELECT address FROM hidden_addresses)
        |  ) AS circulating_supply,
        |  (SELECT COUNT(*) FROM transactions) AS transactions,
        |  (SELECT MAX(height) FROM blocks) AS blocks
      """.stripMargin
    ).as(StatisticsParsers.parseStatistics.single)
  }
}
