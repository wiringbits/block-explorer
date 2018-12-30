package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Count, Limit, PaginatedQuery}
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import javax.inject.Inject
import org.scalactic.Every

class TransactionPostgresDAO @Inject() (fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter) {

  /**
   * NOTE: Ensure the connection has an open transaction.
   */
  def upsert(transaction: Transaction)(implicit conn: Connection): Option[Transaction] = {
    for {
      partialTx <- upsertTransaction(transaction)
      inputs <- upsertInputs(transaction.id, transaction.inputs)
      outputs <- upsertOutputs(transaction.id, transaction.outputs)
      _ <- spend(transaction.id, inputs)
      _ = insertDetails(transaction)
    } yield partialTx.copy(inputs = inputs, outputs = outputs)
  }

  private def spend(txid: TransactionId, inputs: List[Transaction.Input])(implicit conn: Connection): Option[Unit] = {
    val result = inputs.flatMap { input => spend(txid, input) }
    Option(result)
        .filter(_.size == inputs.size)
        .map(_ => ())
  }

  private def spend(txid: TransactionId, input: Transaction.Input)(implicit conn: Connection): Option[Transaction.Output] = {
    SQL(
      """
        |UPDATE transaction_outputs
        |SET spent_on = {spent_on}
        |WHERE txid = {output_txid} AND
        |      index = {output_index}
        |RETURNING txid, index, hex_script, value, address, tpos_owner_address, tpos_merchant_address
      """.stripMargin
    ).on(
      'spent_on -> txid.string,
      'output_txid -> input.fromTxid.string,
      'output_index -> input.fromOutputIndex
    ).as(parseTransactionOutput.single)
  }

  /**
   * NOTE: Ensure the connection has an open transaction.
   */
  def deleteBy(blockhash: Blockhash)(implicit conn: Connection): List[Transaction] = {
    val expectedTransactions = SQL(
      """
        |SELECT txid, blockhash, time, size
        |FROM transactions
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string
    ).as(parseTransaction.*).flatten

    val result = expectedTransactions.map { tx =>
      val inputs = deleteInputs(tx.id)
      val outputs = deleteOutputs(tx.id)
      val _ = deleteDetails(tx.id)

      tx.copy(inputs = inputs, outputs = outputs)
    }

    val deletedTransactions = SQL(
      """
        |DELETE FROM transactions
        |WHERE blockhash = {blockhash}
        |RETURNING txid, blockhash, time, size
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string
    ).as(parseTransaction.*).flatten

    Option(deletedTransactions)
        .filter(_.size == expectedTransactions.size)
        .map(_ => result)
        .getOrElse { throw new RuntimeException("Failed to delete transactions consistently")} // this should not happen
  }

  /**
   * Get the transactions by the given address (sorted by time).
   */
  def getBy(address: Address, limit: Limit, orderingCondition: OrderingCondition)(implicit conn: Connection): List[Transaction] = {
    val order = toSQL(orderingCondition)

    SQL(
      s"""
        |SELECT t.txid, t.blockhash, t.time, t.size
        |FROM transactions t JOIN address_transaction_details USING (txid)
        |WHERE address = {address}
        |ORDER BY time $order
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'address -> address.string,
      'limit -> limit.int
    ).as(parseTransaction.*).flatten
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
      implicit conn: Connection): List[Transaction] = {

    val order = toSQL(orderingCondition)
    val comparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder => ">"
    }

    SQL(
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
        |ORDER BY time $order
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'address -> address.string,
      'limit -> limit.int,
      'lastSeenTxid -> lastSeenTxid.string
    ).executeQuery().as(parseTransaction.*).flatten
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
    ).as(parseTransactionWithValues.*).flatten
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
    ).as(parseTransactionWithValues.*).flatten
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
    ).as(parseTransactionWithValues.*).flatten
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
    ).as(parseTransactionWithValues.*).flatten
  }

  def getUnspentOutputs(address: Address)(implicit conn: Connection): List[Transaction.Output] = {
    SQL(
      """
        |SELECT txid, index, value, address, hex_script, tpos_owner_address, tpos_merchant_address
        |FROM transaction_outputs
        |WHERE address = {address} AND
        |      spent_on IS NULL AND
        |      value > 0
      """.stripMargin
    ).on(
      'address -> address.string
    ).as(parseTransactionOutput.*).flatten
  }

  def getLatestTransactionBy(addresses: Every[Address])(implicit conn: Connection): Map[String, String] = {

    import SqlParser._

    val result = SQL(
      s"""
        |SELECT address, (
        |  SELECT txid
        |  FROM address_transaction_details
        |  WHERE address = a.address
        |  ORDER BY time DESC
        |  LIMIT 1
        |) AS txid
        |FROM address_transaction_details a
        |GROUP BY address
        |HAVING address IN ({addresses});
      """.stripMargin
    ).on(
      'addresses -> addresses.map(_.string).toList
    ).as((str("address") ~ str("txid")).map(flatten).*)

    result.toMap
  }

  private def upsertTransaction(transaction: Transaction)(implicit conn: Connection): Option[Transaction] = {
    SQL(
      """
        |INSERT INTO transactions
        |  (txid, blockhash, time, size)
        |VALUES
        |  ({txid}, {blockhash}, {time}, {size})
        |ON CONFLICT (txid) DO UPDATE
        |  SET blockhash = EXCLUDED.blockhash,
        |      time = EXCLUDED.time,
        |      size = EXCLUDED.size
        |RETURNING txid, blockhash, time, size
      """.stripMargin
    ).on(
      'txid -> transaction.id.string,
      'blockhash -> transaction.blockhash.string,
      'time -> transaction.time,
      'size -> transaction.size.int
    ).as(parseTransaction.singleOpt).flatten
  }

  private def upsertInputs(
      transactionId: TransactionId,
      inputs: List[Transaction.Input])(
      implicit conn: Connection): Option[List[Transaction.Input]] = {

    val result = inputs.map { input =>
      upsertInput(transactionId, input)
    }

    if (result.forall(_.isDefined)) {
      Some(result.flatten)
    } else {
      None
    }
  }

  private def upsertInput(
      transactionId: TransactionId,
      input: Transaction.Input)(
      implicit conn: Connection): Option[Transaction.Input] = {

    SQL(
      """
        |INSERT INTO transaction_inputs
        |  (txid, index, from_txid, from_output_index, value, address)
        |VALUES
        |  ({txid}, {index}, {from_txid}, {from_output_index}, {value}, {address})
        |ON CONFLICT (txid, index) DO UPDATE
        |  SET value = EXCLUDED.value,
        |      address = EXCLUDED.address,
        |      from_txid = EXCLUDED.from_txid,
        |      from_output_index = EXCLUDED.from_output_index
        |RETURNING txid, index, from_txid, from_output_index, value, address
      """.stripMargin
    ).on(
      'txid -> transactionId.string,
      'index -> input.index,
      'from_txid -> input.fromTxid.string,
      'from_output_index -> input.fromOutputIndex,
      'value -> input.value,
      'address -> input.address.string
    ).as(parseTransactionInput.singleOpt).flatten
  }

  private def upsertOutputs(
      transactionId: TransactionId,
      outputs: List[Transaction.Output])(
      implicit conn: Connection): Option[List[Transaction.Output]] = {

    val result = outputs.map { output =>
      upsertOutput(transactionId, output)
    }

    if (result.forall(_.isDefined)) {
      Some(result.flatten)
    } else {
      None
    }
  }

  private def upsertOutput(
      transactionId: TransactionId,
      output: Transaction.Output)(
      implicit conn: Connection): Option[Transaction.Output] = {

    SQL(
      """
        |INSERT INTO transaction_outputs
        |  (txid, index, value, address, hex_script, tpos_owner_address, tpos_merchant_address)
        |VALUES
        |  ({txid}, {index}, {value}, {address}, {hex_script}, {tpos_owner_address}, {tpos_merchant_address})
        |ON CONFLICT (txid, index) DO UPDATE
        |  SET value = EXCLUDED.value,
        |      address = EXCLUDED.address,
        |      hex_script = EXCLUDED.hex_script,
        |      tpos_owner_address = EXCLUDED.tpos_owner_address,
        |      tpos_merchant_address = EXCLUDED.tpos_merchant_address
        |RETURNING txid, index, value, address, hex_script, tpos_owner_address, tpos_merchant_address
      """.stripMargin
    ).on(
      'txid -> transactionId.string,
      'index -> output.index,
      'value -> output.value,
      'address -> output.address.string,
      'hex_script -> output.script.string,
      'tpos_owner_address -> output.tposOwnerAddress.map(_.string),
      'tpos_merchant_address -> output.tposMerchantAddress.map(_.string)
    ).as(parseTransactionOutput.singleOpt).flatten
  }

  private def deleteInputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Input] = {
    SQL(
      """
        |DELETE FROM transaction_inputs
        |WHERE txid = {txid}
        |RETURNING txid, index, from_txid, from_output_index, value, address
      """.stripMargin
    ).on(
      'txid -> txid.string
    ).as(parseTransactionInput.*).flatten
  }

  private def deleteOutputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Output] = {
    val result = SQL(
      """
        |DELETE FROM transaction_outputs
        |WHERE txid = {txid}
        |RETURNING txid, index, hex_script, value, address, tpos_owner_address, tpos_merchant_address
      """.stripMargin
    ).on(
      'txid -> txid.string
    ).as(parseTransactionOutput.*)

    result.flatten
  }

  private def insertDetails(transaction: Transaction)(implicit conn: Connection): List[AddressTransactionDetails] = {
    val received = transaction
        .outputs
        .groupBy(_.address)
        .mapValues { outputs => outputs.map(_.value).sum }
        .map { case (address, value) => AddressTransactionDetails(address, transaction.id, time = transaction.time, received = value) }

    val sent = transaction
        .inputs
        .groupBy(_.address)
        .mapValues { inputs => inputs.map(_.value).sum }
        .map { case (address, value) => AddressTransactionDetails(address, transaction.id, time = transaction.time, sent = value) }

    val result = (received ++ sent)
        .groupBy(_.address)
        .mapValues {
          case head :: list => list.foldLeft(head) { (acc, current) =>
            current.copy(received = current.received + acc.received, sent = current.sent + acc.sent)
          }
        }
        .values
        .map(d => insertDetails(d))

    result.toList
  }

  private def insertDetails(details: AddressTransactionDetails)(implicit conn: Connection): AddressTransactionDetails = {
    SQL(
      """
        |INSERT INTO address_transaction_details
        |  (address, txid, received, sent, time)
        |VALUES
        |  ({address}, {txid}, {received}, {sent}, {time})
        |RETURNING address, txid, received, sent, time
      """.stripMargin
    ).on(
      'address  -> details.address.string,
      'txid -> details.txid.string,
      'received -> details.received,
      'sent -> details.sent,
      'time -> details.time
    ).as(parseAddressTransactionDetails.single)
  }

  private def deleteDetails(txid: TransactionId)(implicit conn: Connection): List[AddressTransactionDetails] = {
    val result = SQL(
      """
        |DELETE FROM address_transaction_details
        |WHERE txid = {txid}
        |RETURNING address, txid, received, sent, time
      """.stripMargin
    ).on(
      'txid -> txid.string
    ).as(parseAddressTransactionDetails.*)

    result
  }

  private def toSQL(condition: OrderingCondition): String = condition match {
    case OrderingCondition.AscendingOrder => "ASC"
    case OrderingCondition.DescendingOrder => "DESC"
  }
}
