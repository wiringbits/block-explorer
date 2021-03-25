package com.xsn.explorer.data.anorm

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.{TransactionOutputPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, Blockhash, TransactionId}
import javax.inject.Inject
import org.scalactic.{Good, One, Or}
import play.api.db.Database

class TransactionPostgresDataHandler @Inject() (
    override val database: Database,
    transactionOutputDAO: TransactionOutputPostgresDAO,
    transactionPostgresDAO: TransactionPostgresDAO
) extends TransactionBlockingDataHandler
    with AnormPostgresDataHandler {

  def getBy(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): ApplicationResult[List[TransactionInfo.HasIO]] = withConnection { implicit conn =>
    val transactions = lastSeenTxid
      .map {
        transactionPostgresDAO.getBy(address, _, limit, orderingCondition)
      }
      .getOrElse {
        transactionPostgresDAO.getBy(address, limit, orderingCondition)
      }

    Good(transactions)
  }

  def getByAddress(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): ApplicationResult[List[TransactionInfo]] = withConnection { implicit conn =>
    val transactions = lastSeenTxid
      .map {
        transactionPostgresDAO.getByAddress(
          address,
          _,
          limit,
          orderingCondition
        )
      }
      .getOrElse {
        transactionPostgresDAO.getByAddress(address, limit, orderingCondition)
      }

    Good(transactions)
  }

  override def getUnspentOutputs(
      address: Address
  ): ApplicationResult[List[Transaction.Output]] = withConnection { implicit conn =>
    val result = transactionOutputDAO.getUnspentOutputs(address)
    Good(result)
  }

  override def getOutput(
      txid: TransactionId,
      index: Int
  ): ApplicationResult[Transaction.Output] = withConnection { implicit conn =>
    val maybe = transactionOutputDAO.getOutput(txid, index)
    Or.from(maybe, One(TransactionError.OutputNotFound(txid, index)))
  }

  override def get(
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition,
      includeZeroValueTransactions: Boolean
  ): ApplicationResult[List[TransactionInfo]] = withConnection { implicit conn =>
    val transactions = lastSeenTxid
      .map { transactionPostgresDAO.get(_, limit, orderingCondition, includeZeroValueTransactions) }
      .getOrElse { transactionPostgresDAO.get(limit, orderingCondition, includeZeroValueTransactions) }

    Good(transactions)
  }

  override def getByBlockhash(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]
  ): ApplicationResult[List[TransactionWithValues]] = withConnection { implicit conn =>
    val transactions = lastSeenTxid
      .map { transactionPostgresDAO.getByBlockhash(blockhash, _, limit) }
      .getOrElse { transactionPostgresDAO.getByBlockhash(blockhash, limit) }

    Good(transactions)
  }

  override def getTransactionsWithIOBy(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]
  ): ApplicationResult[List[Transaction.HasIO]] = withConnection { implicit conn =>
    val transactions = lastSeenTxid
      .map {
        transactionPostgresDAO.getTransactionsWithIOBy(blockhash, _, limit)
      }
      .getOrElse {
        transactionPostgresDAO.getTransactionsWithIOBy(blockhash, limit)
      }

    Good(transactions)
  }
}
