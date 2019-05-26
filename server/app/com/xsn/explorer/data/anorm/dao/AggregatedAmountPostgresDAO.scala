package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._

class AggregatedAmountPostgresDAO {

  def updateAvailableCoins(delta: BigDecimal)(implicit conn: Connection): Unit = {
    val affectedRows = SQL(
      """
        |UPDATE aggregated_amounts
        |SET value = value + {delta}
        |WHERE name = 'available_coins'
      """.stripMargin
    ).on(
        'delta -> delta
      )
      .executeUpdate()

    require(affectedRows == 1)
  }
}
