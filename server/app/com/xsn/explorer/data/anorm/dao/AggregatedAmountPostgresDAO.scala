package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._

class AggregatedAmountPostgresDAO {

  def updateAvailableCoins(
      delta: BigDecimal
  )(implicit conn: Connection): Unit = {
    val affectedRows = SQL(
      """
        |INSERT INTO aggregated_amounts (name, value)
        |VALUES ('available_coins', {delta})
        |ON CONFLICT (name) DO
        |UPDATE SET value = aggregated_amounts.value + EXCLUDED.value
      """.stripMargin
    ).on(
      Symbol("delta") -> delta
    ).executeUpdate()

    require(affectedRows == 1)
  }
}
