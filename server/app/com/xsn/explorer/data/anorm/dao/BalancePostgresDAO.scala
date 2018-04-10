package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.BalanceParsers._
import com.xsn.explorer.models.Balance

class BalancePostgresDAO {

  def upsert(balance: Balance)(implicit conn: Connection): Option[Balance] = {
    SQL(
      """
        |INSERT INTO balances
        |  (address, received, spent, available)
        |VALUES
        |  ({address}, {received}, {spent}, {available})
        |ON CONFLICT (address) DO UPDATE
        |  SET received = balances.received + EXCLUDED.received,
        |      spent = balances.spent + EXCLUDED.spent,
        |      available = balances.available + EXCLUDED.available
        |RETURNING address, received, spent
      """.stripMargin
    ).on(
      'address -> balance.address.string,
      'received -> balance.received,
      'spent -> balance.spent,
      'available -> balance.available
    ).as(parseBalance.singleOpt).flatten
  }
}
