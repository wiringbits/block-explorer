package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.BlockRewardParsers.parseSummary
import com.xsn.explorer.data.anorm.parsers.StatisticsParsers
import com.xsn.explorer.models.{BlockRewardsSummary, Statistics}

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

    result
  }

  def getSummary(numberOfBlocks: Int)(implicit conn: Connection): BlockRewardsSummary = {
    SQL(
      """
        |SELECT
        |  AVG(r.value) as average_reward, AVG(r.staked_amount) as average_input,
        |  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY r.staked_time) as median_wait_time
        |FROM (
        |    SELECT * FROM blocks ORDER BY height DESC LIMIT {number_of_blocks}
        |) b
        |INNER JOIN block_rewards r ON r.blockhash = b.blockhash
      """.stripMargin
    ).on(
        'number_of_blocks -> numberOfBlocks
      )
      .as(parseSummary.single)
  }
}

object StatisticsPostgresDAO {

  /**
   * We need to exclude the burn address from the total supply.
   */
  val BurnAddress = "XmPe9BHRsmZeThtYF34YYjdnrjmcAUn8bC"
}
