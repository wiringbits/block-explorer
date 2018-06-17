package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.{PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.TransactionPostgresDAO
import com.xsn.explorer.errors.{TransactionNotFoundError, TransactionUnknownError}
import com.xsn.explorer.models._
import org.scalactic.{Good, One, Or}
import play.api.db.Database

class TransactionPostgresDataHandler @Inject() (
    override val database: Database,
    transactionPostgresDAO: TransactionPostgresDAO)
    extends TransactionBlockingDataHandler
        with AnormPostgresDataHandler {

  override def upsert(transaction: Transaction): ApplicationResult[Transaction] = withTransaction { implicit conn =>
    val maybe = transactionPostgresDAO.upsert(transaction)
    Or.from(maybe, One(TransactionUnknownError))
  }

  override def delete(transactionId: TransactionId): ApplicationResult[Transaction] = withTransaction { implicit conn =>
    val maybe = transactionPostgresDAO.delete(transactionId)
    Or.from(maybe, One(TransactionNotFoundError))
  }

  override def deleteBy(blockhash: Blockhash): ApplicationResult[List[Transaction]] = withTransaction { implicit conn =>
    val transactions = transactionPostgresDAO.deleteBy(blockhash)
    Good(transactions)
  }

  override def getBy(
      address: Address,
      paginatedQuery: PaginatedQuery): ApplicationResult[PaginatedResult[TransactionWithValues]] = withConnection { implicit conn =>

    val transactions = transactionPostgresDAO.getBy(address, paginatedQuery)
    val total = transactionPostgresDAO.countBy(address)
    val result = PaginatedResult(paginatedQuery.offset, paginatedQuery.limit, total, transactions)

    Good(result)
  }
}
