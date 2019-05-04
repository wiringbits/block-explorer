package com.xsn.explorer.data.anorm

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Limit, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.{TransactionOutputPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, Blockhash, TransactionId}
import javax.inject.Inject
import org.scalactic.{Good, One, Or}
import play.api.db.Database

class TransactionPostgresDataHandler @Inject() (
    override val database: Database,
    transactionOutputDAO: TransactionOutputPostgresDAO,
    transactionPostgresDAO: TransactionPostgresDAO)
    extends TransactionBlockingDataHandler
        with AnormPostgresDataHandler {

  override def getBy(
      address: Address,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = withConnection { implicit conn =>

    val transactions = transactionPostgresDAO.getBy(address, paginatedQuery, ordering)
    val total = transactionPostgresDAO.countBy(address)
    val result = PaginatedResult(paginatedQuery.offset, paginatedQuery.limit, total, transactions)

    Good(result)
  }

  def getBy(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition): ApplicationResult[List[Transaction.HasIO]] = withConnection { implicit conn =>

    val transactions = lastSeenTxid
        .map { transactionPostgresDAO.getBy(address, _, limit, orderingCondition) }
        .getOrElse { transactionPostgresDAO.getBy(address, limit, orderingCondition) }

    Good(transactions)
  }

  override def getUnspentOutputs(address: Address): ApplicationResult[List[Transaction.Output]] = withConnection { implicit conn =>
    val result = transactionOutputDAO.getUnspentOutputs(address)
    Good(result)
  }

  override def getOutput(txid: TransactionId, index: Int): ApplicationResult[Transaction.Output] = withConnection { implicit conn =>
    val maybe = transactionOutputDAO.getOutput(txid, index)
    Or.from(maybe, One(TransactionError.OutputNotFound(txid, index)))
  }

  override def getByBlockhash(
      blockhash: Blockhash,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = withConnection { implicit conn =>

    val transactions = transactionPostgresDAO.getByBlockhash(blockhash, paginatedQuery, ordering)
    val total = transactionPostgresDAO.countByBlockhash(blockhash)
    val result = PaginatedResult(paginatedQuery.offset, paginatedQuery.limit, total, transactions)

    Good(result)
  }

  override def getByBlockhash(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]): ApplicationResult[List[TransactionWithValues]] = withConnection { implicit conn =>

    val transactions = lastSeenTxid
        .map { transactionPostgresDAO.getByBlockhash(blockhash, _, limit) }
        .getOrElse { transactionPostgresDAO.getByBlockhash(blockhash, limit) }

    Good(transactions)
  }

  override def getTransactionsWithIOBy(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]): ApplicationResult[List[Transaction.HasIO]] = withConnection { implicit conn =>

    val transactions = lastSeenTxid
        .map { transactionPostgresDAO.getTransactionsWithIOBy(blockhash, _, limit) }
        .getOrElse { transactionPostgresDAO.getTransactionsWithIOBy(blockhash, limit) }

    Good(transactions)
  }
}
