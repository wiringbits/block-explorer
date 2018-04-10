package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.xsn.explorer.data.anorm.dao.BalancePostgresDAO
import com.xsn.explorer.errors.BalanceUnknownError
import com.xsn.explorer.models.Balance
import org.scalactic.{One, Or}
import play.api.db.Database

class BalancePostgresDataHandler @Inject() (
    override val database: Database,
    balancePostgresDAO: BalancePostgresDAO)
    extends AnormPostgresDataHandler {

  def upsert(balance: Balance) = withConnection { implicit conn =>
    val maybe = balancePostgresDAO.upsert(balance)

    Or.from(maybe, One(BalanceUnknownError))
  }
}
