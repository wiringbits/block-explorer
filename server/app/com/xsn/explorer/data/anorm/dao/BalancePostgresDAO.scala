package com.xsn.explorer.data.anorm.dao

import java.sql.Connection
import javax.inject.Inject

import anorm._
import com.alexitc.playsonify.models.{Count, FieldOrdering, PaginatedQuery}
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.BalanceParsers._
import com.xsn.explorer.models.{Address, Balance}
import com.xsn.explorer.models.fields.BalanceField
import org.slf4j.LoggerFactory

class BalancePostgresDAO @Inject() (fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def upsert(balance: Balance)(implicit conn: Connection): Option[Balance] = {
    val createdBalance = SQL(
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
      'address -> balance.address.string,
      'received -> balance.received,
      'spent -> balance.spent,
    ).as(parseBalance.singleOpt).flatten

    for {
      balance <- createdBalance
      computed <- computeBalance(balance.address) if computed != balance
    } {
      logger.warn(s"CORRUPTED_BALANCE, expected spent = ${computed.spent}, actual = ${balance.spent}, expected received = ${computed.received}, actual = ${balance.received}")
    }

    createdBalance
  }

  private def computeBalance(address: Address)(implicit conn: Connection) = {
    SQL(
      """
        |SELECT {address} AS address,
        |       (
        |         SELECT COALESCE(SUM(value), 0)
        |         FROM transaction_inputs
        |         WHERE address = {address}
        |       ) AS spent,
        |       (
        |         SELECT COALESCE(SUM(value), 0)
        |         FROM transaction_outputs
        |         WHERE address = {address}
        |       ) AS received
      """.stripMargin
    ).on(
      'address -> address.string
    ).as(parseBalance.single)
  }

  def get(
      query: PaginatedQuery,
      ordering: FieldOrdering[BalanceField])(
      implicit conn: Connection): List[Balance] = {

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
    ).as(parseBalance.*).flatten
  }

  def getNonZeroBalances(
      query: PaginatedQuery,
      ordering: FieldOrdering[BalanceField])(
      implicit conn: Connection): List[Balance] = {

    val orderBy = fieldOrderingSQLInterpreter.toOrderByClause(ordering)
    SQL(
      s"""
         |SELECT address, received, spent
         |FROM balances
         |WHERE address NOT IN (
         |  SELECT address
         |  FROM hidden_addresses) AND
         |      (received - spent) > 0
         |$orderBy
         |OFFSET {offset}
         |LIMIT {limit}
      """.stripMargin
    ).on(
      'offset -> query.offset.int,
      'limit -> query.limit.int
    ).as(parseBalance.*).flatten
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

  def countNonZeroBalances(implicit conn: Connection): Count = {
    val result = SQL(
      """
        |SELECT COUNT(*)
        |FROM balances
        |WHERE address NOT IN (
        |  SELECT address
        |  FROM hidden_addresses) AND
        |      (received - spent) > 0
      """.stripMargin
    ).as(SqlParser.scalar[Int].single)

    Count(result)
  }
}
