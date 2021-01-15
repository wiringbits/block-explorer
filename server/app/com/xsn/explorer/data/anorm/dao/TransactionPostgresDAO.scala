package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.models.pagination.{Count, Limit}
import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, Blockhash, TransactionId}
import javax.inject.Inject

class TransactionPostgresDAO @Inject() (
    explorerConfig: ExplorerConfig,
    transactionInputDAO: TransactionInputPostgresDAO,
    transactionOutputDAO: TransactionOutputPostgresDAO,
    tposContractDAO: TPoSContractDAO,
    addressTransactionDetailsDAO: AddressTransactionDetailsPostgresDAO
) {

  /** NOTE: Ensure the connection has an open transaction.
    */
  def upsert(index: Int, transaction: Transaction.HasIO)(implicit
      conn: Connection
  ): Option[Transaction.HasIO] = {
    for {
      partialTx <- upsertTransaction(index, transaction.transaction)
      _ <- transactionOutputDAO.batchInsertOutputs(transaction.outputs)
      _ <- transactionInputDAO.batchInsertInputs(
        transaction.inputs.map(transaction.id -> _)
      )
      _ <- transactionOutputDAO.batchSpend(transaction.id, transaction.inputs)
      _ = closeContracts(List(transaction))
      _ = transactionOutputDAO.batchSpend(transaction.id, transaction.inputs)
      _ <- addressTransactionDetailsDAO.batchInsertDetails(transaction)
    } yield Transaction.HasIO(
      partialTx,
      inputs = transaction.inputs,
      outputs = transaction.outputs
    )
  }

  def insert(
      transactions: List[Transaction.HasIO],
      tposContracts: List[TPoSContract]
  )(implicit
      conn: Connection
  ): Option[List[Transaction]] = {
    for {
      r <- batchInsert(transactions.map(_.transaction))

      outputs = transactions.flatMap(_.outputs)
      _ <- transactionOutputDAO.batchInsertOutputs(outputs)

      inputs = transactions.flatMap { tx =>
        tx.inputs.map(tx.id -> _)
      }
      _ <- transactionInputDAO.batchInsertInputs(inputs)
    } yield {
      insertDetails(transactions)
      spend(transactions)
      closeContracts(transactions)
      tposContracts.foreach { contract =>
        tposContractDAO.create(contract)
      }
      r
    }
  }

  private def insertDetails(
      transactions: List[Transaction.HasIO]
  )(implicit conn: Connection): Unit = {
    val detailsResult =
      transactions.map(addressTransactionDetailsDAO.batchInsertDetails)

    assert(
      detailsResult.forall(_.isDefined),
      "Inserting address details batch failed"
    )
  }

  private def spend(
      transactions: List[Transaction.HasIO]
  )(implicit conn: Connection): Unit = {
    val spendResult = transactions.map { tx =>
      transactionOutputDAO.batchSpend(tx.id, tx.inputs)
    }

    if (explorerConfig.liteVersionConfig.enabled) {
      ()
    } else {
      assert(spendResult.forall(_.isDefined), "Spending inputs batch failed")
    }
  }

  private def closeContracts(
      transactions: List[Transaction.HasIO]
  )(implicit conn: Connection): Unit = {
    for {
      tx <- transactions
      // a contract requires 1 XSN
      input <- tx.inputs if input.value == 1
    } {
      val id = TPoSContract.Id(input.fromTxid, input.fromOutputIndex)
      tposContractDAO.close(id, tx.id)
    }
  }

  private def batchInsert(
      transactions: List[Transaction]
  )(implicit conn: Connection): Option[List[Transaction]] = {
    transactions match {
      case Nil => Some(transactions)
      case _ =>
        val params = transactions.zipWithIndex.map {
          case (transaction, index) =>
            List(
              'txid -> transaction.id.toBytesBE.toArray: NamedParameter,
              'blockhash -> transaction.blockhash.toBytesBE.toArray: NamedParameter,
              'time -> transaction.time: NamedParameter,
              'size -> transaction.size.int: NamedParameter,
              'index -> index: NamedParameter
            )
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

  /** NOTE: Ensure the connection has an open transaction.
    */
  def deleteBy(
      blockhash: Blockhash
  )(implicit conn: Connection): List[Transaction.HasIO] = {
    val expectedTransactions = SQL(
      """
        |SELECT txid, blockhash, time, size
        |FROM transactions
        |WHERE blockhash = {blockhash}
        |ORDER BY index DESC
      """.stripMargin
    ).on(
      'blockhash -> blockhash.toBytesBE.toArray
    ).as(parseTransaction.*)

    val result = expectedTransactions.map { tx =>
      val _ = (
        tposContractDAO.deleteBy(tx.id),
        addressTransactionDetailsDAO.deleteDetails(tx.id)
      )
      val inputs = transactionInputDAO.deleteInputs(tx.id)
      val outputs = transactionOutputDAO.deleteOutputs(tx.id)

      inputs
        .map { input =>
          TPoSContract.Id(input.fromTxid, input.fromOutputIndex)
        }
        .foreach(tposContractDAO.open(_))

      Transaction.HasIO(tx, inputs = inputs, outputs = outputs)
    }

    val deletedTransactions = SQL(
      """
        |DELETE FROM transactions
        |WHERE blockhash = {blockhash}
        |RETURNING txid, blockhash, time, size
      """.stripMargin
    ).on(
      'blockhash -> blockhash.toBytesBE.toArray
    ).as(parseTransaction.*)

    Option(deletedTransactions)
      .filter(_.size == expectedTransactions.size)
      .map(_ => result)
      .getOrElse {
        throw new RuntimeException("Failed to delete transactions consistently")
      } // this should not happen
  }

  /** Get the transactions by the given address (sorted by time).
    */
  def getBy(
      address: Address,
      limit: Limit,
      orderingCondition: OrderingCondition
  )(implicit
      conn: Connection
  ): List[Transaction.HasIO] = {
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

  def getByAddress(
      address: Address,
      limit: Limit,
      orderingCondition: OrderingCondition
  )(implicit
      conn: Connection
  ): List[TransactionInfo] = {
    val order = toSQL(orderingCondition)

    SQL(
      s"""
        |WITH TXS AS(
        | SELECT t.txid, t.blockhash, t.time, t.size
        | FROM transactions t JOIN address_transaction_details USING (txid)
        | WHERE address = {address}
        | ORDER BY time $order, txid
        | LIMIT {limit}
        |)
        |SELECT t.txid, t.blockhash, t.time, t.size, blk.height,
        | (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        | (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM TXS t JOIN blocks blk USING (blockhash)
      """.stripMargin
    ).on(
      'address -> address.string,
      'limit -> limit.int
    ).as(parseTransactionInfo.*)

  }

  /** Get the transactions by the given address (sorted by time).
    *
    * - When orderingCondition = DescendingOrder, the transactions that occurred before the last seen transaction are retrieved.
    * - When orderingCondition = AscendingOrder, the transactions that occurred after the last seen transaction are retrieved.
    */
  def getBy(
      address: Address,
      lastSeenTxid: TransactionId,
      limit: Limit,
      orderingCondition: OrderingCondition
  )(implicit
      conn: Connection
  ): List[Transaction.HasIO] = {

    val order = toSQL(orderingCondition)
    val comparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder  => ">"
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
      'lastSeenTxid -> lastSeenTxid.toBytesBE.toArray
    ).as(parseTransaction.*)

    for {
      tx <- transactions
    } yield {
      val inputs = transactionInputDAO.getInputs(tx.id, address)
      val outputs = transactionOutputDAO.getOutputs(tx.id, address)
      Transaction.HasIO(tx, inputs = inputs, outputs = outputs)
    }
  }

  def getByAddress(
      address: Address,
      lastSeenTxid: TransactionId,
      limit: Limit,
      orderingCondition: OrderingCondition
  )(implicit
      conn: Connection
  ): List[TransactionInfo] = {

    val order = toSQL(orderingCondition)
    val comparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder  => ">"
    }

    SQL(
      s"""
        |WITH CTE AS (
        |  SELECT time AS lastSeenTime
        |  FROM transactions
        |  WHERE txid = {lastSeenTxid}
        |),
        |TXS AS(
        | SELECT t.txid, t.blockhash, t.time, t.size
        | FROM CTE CROSS JOIN transactions t
        |          JOIN address_transaction_details USING (txid)
        | WHERE address = {address} AND
        |       (t.time $comparator lastSeenTime OR (t.time = lastSeenTime AND t.txid > {lastSeenTxid}))
        | ORDER BY time $order, txid
        | LIMIT {limit}
        |)
        |SELECT t.txid, t.blockhash, t.time, t.size, blk.height,
        | (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        | (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM TXS t JOIN blocks blk USING (blockhash)
      """.stripMargin
    ).on(
      'address -> address.string,
      'limit -> limit.int,
      'lastSeenTxid -> lastSeenTxid.toBytesBE.toArray
    ).as(parseTransactionInfo.*)
  }

  def countByBlockhash(
      blockhash: Blockhash
  )(implicit conn: Connection): Count = {
    val result = SQL(
      """
        |SELECT COUNT(*)
        |FROM blocks JOIN transactions USING (blockhash)
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on(
      'blockhash -> blockhash.toBytesBE.toArray
    ).as(SqlParser.scalar[Int].single)

    Count(result)
  }

  def getByBlockhash(blockhash: Blockhash, limit: Limit)(implicit
      conn: Connection
  ): List[TransactionWithValues] = {
    SQL(
      """
        |SELECT t.txid, t.blockhash, t.time, t.size,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM transactions t JOIN blocks USING (blockhash)
        |WHERE blockhash = {blockhash}
        |ORDER BY t.index ASC
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'limit -> limit.int,
      'blockhash -> blockhash.toBytesBE.toArray
    ).as(parseTransactionWithValues.*)
  }

  def getByBlockhash(
      blockhash: Blockhash,
      lastSeenTxid: TransactionId,
      limit: Limit
  )(implicit
      conn: Connection
  ): List[TransactionWithValues] = {
    SQL(
      """
        |WITH CTE AS (
        |  SELECT index AS lastSeenIndex
        |  FROM transactions
        |  WHERE txid = {lastSeenTxid}
        |)
        |SELECT t.txid, t.blockhash, t.time, t.size,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM CTE CROSS JOIN transactions t JOIN blocks USING (blockhash)
        |WHERE blockhash = {blockhash} AND
        |      t.index > lastSeenIndex
        |ORDER BY t.index ASC
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'limit -> limit.int,
      'blockhash -> blockhash.toBytesBE.toArray,
      'lastSeenTxid -> lastSeenTxid.toBytesBE.toArray
    ).as(parseTransactionWithValues.*)
  }

  def get(limit: Limit, orderingCondition: OrderingCondition)(implicit
      conn: Connection
  ): List[TransactionInfo] = {

    val order = toSQL(orderingCondition)

    SQL(
      s"""
        |WITH TXS AS (
        |   SELECT txid, blockhash, time, size
        |   FROM transactions
        |   ORDER BY time $order, txid
        |   LIMIT {limit}
        |)
        |SELECT t.txid, t.blockhash, t.time, t.size, blk.height,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM TXS t JOIN blocks blk USING (blockhash)
      """.stripMargin
    ).on(
      'limit -> limit.int
    ).as(parseTransactionInfo.*)
  }

  def get(
      lastSeenTxid: TransactionId,
      limit: Limit,
      orderingCondition: OrderingCondition
  )(implicit
      conn: Connection
  ): List[TransactionInfo] = {

    val order = toSQL(orderingCondition)
    val timeComparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder  => ">"
    }
    val indexComparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => ">"
      case OrderingCondition.AscendingOrder  => "<"
    }

    SQL(
      s"""
        |WITH CTE AS (
        |  SELECT index AS lastSeenIndex, time AS lastSeenTime
        |  FROM transactions
        |  WHERE txid = {lastSeenTxid}
        |),
        |TXS AS (
        |  SELECT txid, blockhash, time, size, index
        |  FROM CTE CROSS JOIN transactions
        |  WHERE time $timeComparator lastSeenTime
        |  OR (time = lastSeenTime AND index $indexComparator lastSeenIndex)
        |  ORDER BY time $order, txid
        |  LIMIT {limit}
        |)
        |SELECT t.txid, t.blockhash, t.time, t.size, blk.height,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM TXS t JOIN blocks blk USING (blockhash)
      """.stripMargin
    ).on(
      'limit -> limit.int,
      'lastSeenTxid -> lastSeenTxid.toBytesBE.toArray
    ).as(parseTransactionInfo.*)
  }

  def getTransactionsWithIOBy(blockhash: Blockhash, limit: Limit)(implicit
      conn: Connection
  ): List[Transaction.HasIO] = {
    val transactions = SQL(
      """
        |SELECT t.txid, t.blockhash, t.time, t.size
        |FROM transactions t JOIN blocks USING (blockhash)
        |WHERE blockhash = {blockhash}
        |ORDER BY t.index ASC
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'limit -> limit.int,
      'blockhash -> blockhash.toBytesBE.toArray
    ).as(parseTransaction.*)

    for {
      tx <- transactions
    } yield {
      val inputs = transactionInputDAO.getInputs(tx.id)
      val outputs = transactionOutputDAO.getOutputs(tx.id)
      Transaction.HasIO(tx, inputs = inputs, outputs = outputs)
    }
  }

  def getTransactionsWithIOBy(
      blockhash: Blockhash,
      lastSeenTxid: TransactionId,
      limit: Limit
  )(implicit
      conn: Connection
  ): List[Transaction.HasIO] = {
    val transactions = SQL(
      """
        |WITH CTE AS (
        |  SELECT index AS lastSeenIndex
        |  FROM transactions
        |  WHERE txid = {lastSeenTxid}
        |)
        |SELECT t.txid, t.blockhash, t.time, t.size
        |FROM CTE CROSS JOIN transactions t JOIN blocks USING (blockhash)
        |WHERE blockhash = {blockhash} AND
        |      t.index > lastSeenIndex
        |ORDER BY t.index ASC
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'limit -> limit.int,
      'blockhash -> blockhash.toBytesBE.toArray,
      'lastSeenTxid -> lastSeenTxid.toBytesBE.toArray
    ).as(parseTransaction.*)

    for {
      tx <- transactions
    } yield {
      val inputs = transactionInputDAO.getInputs(tx.id)
      val outputs = transactionOutputDAO.getOutputs(tx.id)
      Transaction.HasIO(tx, inputs = inputs, outputs = outputs)
    }
  }

  def upsertTransaction(index: Int, transaction: Transaction)(implicit
      conn: Connection
  ): Option[Transaction] = {
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
      'txid -> transaction.id.toBytesBE.toArray,
      'blockhash -> transaction.blockhash.toBytesBE.toArray,
      'time -> transaction.time,
      'size -> transaction.size.int,
      'index -> index
    ).as(parseTransaction.singleOpt)
  }

  private def toSQL(condition: OrderingCondition): String = condition match {
    case OrderingCondition.AscendingOrder  => "ASC"
    case OrderingCondition.DescendingOrder => "DESC"
  }
}
