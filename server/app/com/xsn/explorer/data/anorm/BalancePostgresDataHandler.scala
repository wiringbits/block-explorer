package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.BalanceBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.BalancePostgresDAO
import com.xsn.explorer.errors.BalanceUnknownError
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.fields.BalanceField
import org.scalactic.{Good, One, Or}
import play.api.db.Database

class BalancePostgresDataHandler @Inject() (
    override val database: Database,
    balancePostgresDAO: BalancePostgresDAO)
    extends BalanceBlockingDataHandler
    with AnormPostgresDataHandler {

  override def upsert(balance: Balance): ApplicationResult[Balance] = withConnection { implicit conn =>
    val maybe = balancePostgresDAO.upsert(balance)

    Or.from(maybe, One(BalanceUnknownError))
  }

  override def get(
      query: PaginatedQuery,
      ordering: FieldOrdering[BalanceField]): ApplicationResult[PaginatedResult[Balance]] = withConnection { implicit conn =>

    val balances = balancePostgresDAO.get(query, ordering)
    val total = balancePostgresDAO.count
    val result = PaginatedResult(query.offset, query.limit, total, balances)

    Good(result)
  }

  override def getNonZeroBalances(
      query: PaginatedQuery,
      ordering: FieldOrdering[BalanceField]): ApplicationResult[PaginatedResult[Balance]] = withConnection { implicit conn =>

    val balances = balancePostgresDAO.getNonZeroBalances(query, ordering)
    val total = balancePostgresDAO.countNonZeroBalances
    val result = PaginatedResult(query.offset, query.limit, total, balances)

    Good(result)
  }
}
