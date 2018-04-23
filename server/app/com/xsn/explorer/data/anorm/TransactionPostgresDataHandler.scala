package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.anorm.dao.TransactionPostgresDAO
import com.xsn.explorer.errors.{TransactionNotFoundError, TransactionUnknownError}
import com.xsn.explorer.models.{Blockhash, Transaction, TransactionId}
import org.scalactic.{Good, One, Or}
import play.api.db.Database

class TransactionPostgresDataHandler @Inject() (
    override val database: Database,
    transactionPostgresDAO: TransactionPostgresDAO)
    extends AnormPostgresDataHandler {

  def upsert(transaction: Transaction): ApplicationResult[Transaction] = withTransaction { implicit conn =>
    val maybe = transactionPostgresDAO.upsert(transaction)
    Or.from(maybe, One(TransactionUnknownError))
  }

  def delete(transactionId: TransactionId): ApplicationResult[Transaction] = withTransaction { implicit conn =>
    val maybe = transactionPostgresDAO.delete(transactionId)
    Or.from(maybe, One(TransactionNotFoundError))
  }

  def deleteBy(blockhash: Blockhash) = withTransaction { implicit conn =>
    val transactions = transactionPostgresDAO.deleteBy(blockhash)
    Good(transactions)
  }
}
