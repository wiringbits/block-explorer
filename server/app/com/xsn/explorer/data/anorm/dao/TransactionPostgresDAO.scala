package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Count, Limit, PaginatedQuery}
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, Blockhash, TransactionId}
import javax.inject.Inject
import org.slf4j.LoggerFactory

class TransactionPostgresDAO @Inject() (
    transactionInputDAO: TransactionInputPostgresDAO,
    transactionOutputDAO: TransactionOutputPostgresDAO,
    addressTransactionDetailsDAO: AddressTransactionDetailsPostgresDAO,
    fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * NOTE: Ensure the connection has an open transaction.
   */
  def upsert(index: Int, transaction: Transaction.HasIO)(implicit conn: Connection): Option[Transaction.HasIO] = {
    for {
      partialTx <- upsertTransaction(index, transaction.transaction)
      _ <- transactionOutputDAO.batchInsertOutputs(transaction.outputs)
      _ <- transactionInputDAO.batchInsertInputs(transaction.inputs.map(transaction.id -> _))
      _ <- transactionOutputDAO.batchSpend(transaction.id, transaction.inputs)
      _ <- addressTransactionDetailsDAO.batchInsertDetails(transaction)
    } yield Transaction.HasIO(partialTx, inputs = transaction.inputs, outputs = transaction.outputs)
  }

  def insert(transactions: List[Transaction.HasIO])(implicit conn: Connection): Option[List[Transaction]] = {
    for {
      r <- batchInsert(transactions.map(_.transaction))

      outputs = transactions.flatMap(_.outputs)
      _ <- transactionOutputDAO.batchInsertOutputs(outputs)

      inputs = transactions.flatMap { tx => tx.inputs.map(tx.id -> _) }
      _ <- transactionInputDAO.batchInsertInputs(inputs)
    } yield {
      insertDetails(transactions)
      spend(transactions)
      r
    }
  }

  private def insertDetails(transactions: List[Transaction.HasIO])(implicit conn: Connection): Unit = {
    val detailsResult = transactions.map(addressTransactionDetailsDAO.batchInsertDetails)

    assert(detailsResult.forall(_.isDefined), "Inserting address details batch failed")
  }

  private def spend(transactions: List[Transaction.HasIO])(implicit conn: Connection): Unit = {
    val spendResult = transactions.map { tx => transactionOutputDAO.batchSpend(tx.id, tx.inputs) }

    assert(spendResult.forall(_.isDefined), "Spending inputs batch failed")
  }

  private def batchInsert(transactions: List[Transaction])(implicit conn: Connection): Option[List[Transaction]] = {
    transactions match {
      case Nil => Some(transactions)
      case _ =>
        val params = transactions.zipWithIndex.map { case (transaction, index) =>
          List(
            'txid -> transaction.id.string: NamedParameter,
            'blockhash -> transaction.blockhash.string: NamedParameter,
            'time -> transaction.time: NamedParameter,
            'size -> transaction.size.int: NamedParameter,
            'index -> index: NamedParameter)
        }

        val batch = BatchSql(
          """
            |INSERT INTO transactions
            |  (txid, blockhash, time, size, index)
            |VALUES
            |  ({txid}, {blockhash}, {time}, {size}, {index})
          """.stripMargin,
          params.head,
          params.tail: _*
        )

        val success = batch.execute().forall(_ == 1)
        if (success) {
          Some(transactions)
        } else {
          None
        }
    }
  }

  /**
   * NOTE: Ensure the connection has an open transaction.
   */
  def deleteBy(blockhash: Blockhash)(implicit conn: Connection): List[Transaction.HasIO] = {
    val expectedTransactions = SQL(
      """
        |SELECT txid, blockhash, time, size
        |FROM transactions
        |WHERE blockhash = {blockhash}
        |ORDER BY index DESC
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string
    ).as(parseTransaction.*)

    val result = expectedTransactions.map { tx =>
      val inputs = transactionInputDAO.deleteInputs(tx.id)
      val outputs = transactionOutputDAO.deleteOutputs(tx.id)
      val _ = addressTransactionDetailsDAO.deleteDetails(tx.id)

      Transaction.HasIO(tx, inputs = inputs, outputs = outputs)
    }

    val deletedTransactions = SQL(
      """
        |DELETE FROM transactions
        |WHERE blockhash = {blockhash}
        |RETURNING txid, blockhash, time, size
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string
    ).as(parseTransaction.*)

    Option(deletedTransactions)
        .filter(_.size == expectedTransactions.size)
        .map(_ => result)
        .getOrElse { throw new RuntimeException("Failed to delete transactions consistently")} // this should not happen
  }

  /**
   * Get the transactions by the given address (sorted by time).
   */
  def getBy(address: Address, limit: Limit, orderingCondition: OrderingCondition)(implicit conn: Connection): List[Transaction.HasIO] = {
    val order = toSQL(orderingCondition)

    val transactions = SQL(
      s"""
        |SELECT t.txid, t.blockhash, t.time, t.size
        |FROM transactions t JOIN address_transaction_details USING (txid)
        |WHERE address = {address}
        |ORDER BY time $order, txid
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'address -> address.string,
      'limit -> limit.int
    ).as(parseTransaction.*)

    for {
      tx <- transactions
    } yield {
      val inputs = transactionInputDAO.getInputs(tx.id, address)
      val outputs = transactionOutputDAO.getOutputs(tx.id, address)
      Transaction.HasIO(tx, inputs = inputs, outputs = outputs)
    }
  }

  /**
   * Get the transactions by the given address (sorted by time).
   *
   * - When orderingCondition = DescendingOrder, the transactions that occurred before the last seen transaction are retrieved.
   * - When orderingCondition = AscendingOrder, the transactions that occurred after the last seen transaction are retrieved.
   */
  def getBy(
      address: Address,
      lastSeenTxid: TransactionId,
      limit: Limit,
      orderingCondition: OrderingCondition)(
      implicit conn: Connection): List[Transaction.HasIO] = {

    val order = toSQL(orderingCondition)
    val comparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder => ">"
    }

    val transactions = SQL(
      s"""
        |WITH CTE AS (
        |  SELECT time AS lastSeenTime
        |  FROM transactions
        |  WHERE txid = {lastSeenTxid}
        |)
        |SELECT t.txid, t.blockhash, t.time, t.size
        |FROM CTE CROSS JOIN transactions t
        |         JOIN address_transaction_details USING (txid)
        |WHERE address = {address} AND
        |      (t.time $comparator lastSeenTime OR (t.time = lastSeenTime AND t.txid > {lastSeenTxid}))
        |ORDER BY time $order, txid
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'address -> address.string,
      'limit -> limit.int,
      'lastSeenTxid -> lastSeenTxid.string
    ).as(parseTransaction.*)

    for {
      tx <- transactions
    } yield {
      val inputs = transactionInputDAO.getInputs(tx.id, address)
      val outputs = transactionOutputDAO.getOutputs(tx.id, address)
      Transaction.HasIO(tx, inputs = inputs, outputs = outputs)
    }
  }

  def getBy(
      address: Address,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField])(
      implicit conn: Connection): List[TransactionWithValues] = {

    val orderBy = fieldOrderingSQLInterpreter.toOrderByClause(ordering)

    SQL(
      s"""
         |SELECT t.txid, t.blockhash, t.time, t.size, a.sent, a.received
         |FROM transactions t
         |INNER JOIN address_transaction_details a USING (txid)
         |WHERE a.address = {address}
         |$orderBy
         |OFFSET {offset}
         |LIMIT {limit}
      """.stripMargin
    ).on(
      'address -> address.string,
      'offset -> paginatedQuery.offset.int,
      'limit -> paginatedQuery.limit.int
    ).as(parseTransactionWithValues.*)
  }

  def countBy(address: Address)(implicit conn: Connection): Count = {
    val result = SQL(
      """
        |  SELECT COUNT(*)
        |  FROM address_transaction_details
        |  WHERE address = {address}
      """.stripMargin
    ).on(
      'address -> address.string
    ).as(SqlParser.scalar[Int].single)

    Count(result)
  }

  def getByBlockhash(
      blockhash: Blockhash,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField])(
      implicit conn: Connection): List[TransactionWithValues] = {

    val orderBy = fieldOrderingSQLInterpreter.toOrderByClause(ordering)

    /**
     * TODO: The query is very slow while aggregating the spent and received values,
     *       it might be worth creating an index-like table to get the accumulated
     *       values directly.
     */
    SQL(
      s"""
         |SELECT t.txid, blockhash, t.time, t.size,
         |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
         |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
         |FROM transactions t JOIN blocks USING (blockhash)
         |WHERE blockhash = {blockhash}
         |$orderBy
         |OFFSET {offset}
         |LIMIT {limit}
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string,
      'offset -> paginatedQuery.offset.int,
      'limit -> paginatedQuery.limit.int
    ).as(parseTransactionWithValues.*)
  }

  def countByBlockhash(blockhash: Blockhash)(implicit conn: Connection): Count = {
    val result = SQL(
      """
        |SELECT COUNT(*)
        |FROM blocks JOIN transactions USING (blockhash)
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string
    ).as(SqlParser.scalar[Int].single)

    Count(result)
  }

  def getByBlockhash(blockhash: Blockhash, limit: Limit)(implicit conn: Connection): List[TransactionWithValues] = {
    SQL(
      """
        |SELECT t.txid, t.blockhash, t.time, t.size,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM transactions t JOIN blocks USING (blockhash)
        |WHERE blockhash = {blockhash}
        |ORDER BY t.txid ASC
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'limit -> limit.int,
      'blockhash -> blockhash.string
    ).as(parseTransactionWithValues.*)
  }

  def getByBlockhash(blockhash: Blockhash, lastSeenTxid: TransactionId, limit: Limit)(implicit conn: Connection): List[TransactionWithValues] = {
    SQL(
      """
        |SELECT t.txid, t.blockhash, t.time, t.size,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM transactions t JOIN blocks USING (blockhash)
        |WHERE blockhash = {blockhash} AND
        |      t.txid > {lastSeenTxid}
        |ORDER BY t.txid ASC
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'limit -> limit.int,
      'blockhash -> blockhash.string,
      'lastSeenTxid -> lastSeenTxid.string
    ).as(parseTransactionWithValues.*)
  }

  private def upsertTransaction(index: Int, transaction: Transaction)(implicit conn: Connection): Option[Transaction] = {
    SQL(
      """
        |INSERT INTO transactions
        |  (txid, blockhash, time, size, index)
        |VALUES
        |  ({txid}, {blockhash}, {time}, {size}, {index})
        |ON CONFLICT (txid) DO UPDATE
        |  SET blockhash = EXCLUDED.blockhash,
        |      time = EXCLUDED.time,
        |      size = EXCLUDED.size,
        |      index = EXCLUDED.index
        |RETURNING txid, blockhash, time, size
      """.stripMargin
    ).on(
      'txid -> transaction.id.string,
      'blockhash -> transaction.blockhash.string,
      'time -> transaction.time,
      'size -> transaction.size.int,
      'index -> index
    ).as(parseTransaction.singleOpt)
  }

  private def toSQL(condition: OrderingCondition): String = condition match {
    case OrderingCondition.AscendingOrder => "ASC"
    case OrderingCondition.DescendingOrder => "DESC"
  }
}
