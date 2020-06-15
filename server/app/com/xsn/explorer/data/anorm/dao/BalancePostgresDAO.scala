package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.playsonify.models.ordering.FieldOrdering
import com.alexitc.playsonify.models.pagination.{Count, Limit, PaginatedQuery}
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.BalanceParsers._
import com.xsn.explorer.models.fields.BalanceField
import com.xsn.explorer.models.persisted.Balance
import com.xsn.explorer.models.values.Address
import javax.inject.Inject

class BalancePostgresDAO @Inject()(fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter) {

  /**
   * create or update the balance for an address
   */
  def upsert(partial: Balance)(implicit conn: Connection): Option[Balance] = {
    SQL(
      """
        |INSERT INTO balances
        |  (address, received, spent)
        |VALUES
        |  ({address}, {received}, {spent})
        |ON CONFLICT (address) DO UPDATE
        |  SET received = balances.received + EXCLUDED.received,
        |      spent = balances.spent + EXCLUDED.spent
        |RETURNING address, received, spent
      """.stripMargin
    ).on(
        'address -> partial.address.string,
        'received -> partial.received,
        'spent -> partial.spent
      )
      .as(parseBalance.singleOpt)
  }

  def get(query: PaginatedQuery, ordering: FieldOrdering[BalanceField])(implicit conn: Connection): List[Balance] = {

    val orderBy = fieldOrderingSQLInterpreter.toOrderByClause(ordering)
    SQL(
      s"""
        |SELECT address, received, spent
        |FROM balances
        |WHERE address NOT IN (
        |  SELECT address
        |  FROM hidden_addresses
        |)
        |$orderBy
        |OFFSET {offset}
        |LIMIT {limit}
      """.stripMargin
    ).on(
        'offset -> query.offset.int,
        'limit -> query.limit.int
      )
      .as(parseBalance.*)
  }

  def count(implicit conn: Connection): Count = {
    val result = SQL(
      """
        |SELECT COUNT(*)
        |FROM balances
        |WHERE address NOT IN (
        |  SELECT address
        |  FROM hidden_addresses
        |)
      """.stripMargin
    ).as(SqlParser.scalar[Int].single)

    Count(result)
  }

  def getBy(address: Address)(implicit conn: Connection): Option[Balance] = {
    SQL(
      s"""
         |SELECT address, received, spent
         |FROM balances
         |wHERE address = {address}
      """.stripMargin
    ).on(
        'address -> address.string
      )
      .as(parseBalance.singleOpt)
  }

  /**
   * Get the highest balances (excluding hidden_addresses).
   */
  def getHighestBalances(limit: Limit)(implicit conn: Connection): List[Balance] = {
    SQL(
      """
        |SELECT address, received, spent
        |FROM balances
        |WHERE address NOT IN (SELECT address FROM hidden_addresses)
        |ORDER BY (received - spent) DESC
        |LIMIT {limit}
      """.stripMargin
    ).on(
        'limit -> limit.int
      )
      .as(parseBalance.*)
  }

  /**
   * Get the highest balances excluding the balances until the given address (excluding hidden_addresses).
   *
   * Note, the results across calls might not be stable if the given address changes its balance drastically.
   */
  def getHighestBalances(lastSeenAddress: Address, limit: Limit)(implicit conn: Connection): List[Balance] = {
    SQL(
      """
        |WITH CTE AS (
        |  SELECT (received - spent) AS lastSeenAvailable
        |  FROM balances
        |  WHERE address = {lastSeenAddress}
        |)
        |SELECT address, received, spent
        |FROM CTE CROSS JOIN balances
        |WHERE ((received - spent) < lastSeenAvailable OR
        |      ((received - spent) = lastSeenAvailable AND address > {lastSeenAddress})) AND
        |      address NOT IN (SELECT address FROM hidden_addresses)
        |ORDER BY (received - spent) DESC
        |LIMIT {limit}
      """.stripMargin
    ).on(
        'limit -> limit.int,
        'lastSeenAddress -> lastSeenAddress.string
      )
      .as(parseBalance.*)
  }
}
