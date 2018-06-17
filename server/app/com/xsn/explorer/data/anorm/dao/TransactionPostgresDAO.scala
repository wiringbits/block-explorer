package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.playsonify.models.{Count, PaginatedQuery}
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models._

class TransactionPostgresDAO {

  /**
   * NOTE: Ensure the connection has an open transaction.
   */
  def upsert(transaction: Transaction)(implicit conn: Connection): Option[Transaction] = {
    for {
      partialTx <- upsertTransaction(transaction)
      inputs <- upsertInputs(transaction.id, transaction.inputs)
      outputs <- upsertOutputs(transaction.id, transaction.outputs)
    } yield partialTx.copy(inputs = inputs, outputs = outputs)
  }

  /**
   * NOTE: Ensure the connection has an open transaction.
   */
  def delete(txid: TransactionId)(implicit conn: Connection): Option[Transaction] = {
    val inputs = deleteInputs(txid)
    val outputs = deleteOutputs(txid)

    val txMaybe = SQL(
      """
        |DELETE FROM transactions
        |WHERE txid = {txid}
        |RETURNING txid, blockhash, time, size
      """.stripMargin
    ).on(
      'txid -> txid.string
    ).as(parseTransaction.singleOpt)

    for {
      tx <- txMaybe.flatten
    } yield tx.copy(inputs = inputs, outputs = outputs)
  }

  /**
   * NOTE: Ensure the connection has an open transaction.
   */
  def deleteBy(blockhash: Blockhash)(implicit conn: Connection): List[Transaction] = {
    val transactions = SQL(
      """
        |DELETE FROM transactions
        |WHERE blockhash = {blockhash}
        |RETURNING txid, blockhash, time, size
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string
    ).as(parseTransaction.*).flatten

    transactions.map { tx =>
      val inputs = deleteInputs(tx.id)
      val outputs = deleteOutputs(tx.id)

      tx.copy(inputs = inputs, outputs = outputs)
    }
  }

  def getBy(address: Address, paginatedQuery: PaginatedQuery)(implicit conn: Connection): List[TransactionWithValues] = {
    /**
     * TODO: The query is very slow while aggregating the spent and received values,
     *       it might be worth creating an index-like table to get the accumulated
     *       values directly.
     */
    SQL(
      """
        |SELECT t.txid, blockhash, time, size,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid) AS received
        |FROM transactions t
        |WHERE t.txid IN (
        |  SELECT txid
        |  FROM transaction_inputs
        |  WHERE address = {address}
        |) OR t.txid IN (
        |  SELECT txid
        |  FROM transaction_outputs
        |  WHERE address = {address}
        |)
        |ORDER BY time DESC
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
        |SELECT COUNT(*)
        |FROM transactions
        |WHERE txid IN (
        |  SELECT txid
        |  FROM transaction_inputs
        |  WHERE address = {address}
        |) OR txid IN (
        |  SELECT txid
        |  FROM transaction_outputs
        |  WHERE address = {address}
        |)
      """.stripMargin
    ).on(
      'address -> address.string
    ).as(SqlParser.scalar[Int].single)

    Count(result)
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
        |  (txid, index, value, address)
        |VALUES
        |  ({txid}, {index}, {value}, {address})
        |ON CONFLICT (txid, index) DO UPDATE
        |  SET value = EXCLUDED.value,
        |      address = EXCLUDED.address
        |RETURNING index, value, address
      """.stripMargin
    ).on(
      'txid -> transactionId.string,
      'index -> input.index,
      'value -> input.value,
      'address -> input.address.map(_.string)
    ).as(parseTransactionInput.singleOpt)
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
        |  (txid, index, value, address, tpos_owner_address, tpos_merchant_address)
        |VALUES
        |  ({txid}, {index}, {value}, {address}, {tpos_owner_address}, {tpos_merchant_address})
        |ON CONFLICT (txid, index) DO UPDATE
        |  SET value = EXCLUDED.value,
        |      address = EXCLUDED.address,
        |      tpos_owner_address = EXCLUDED.tpos_owner_address,
        |      tpos_merchant_address = EXCLUDED.tpos_merchant_address
        |RETURNING index, value, address, tpos_owner_address, tpos_merchant_address
      """.stripMargin
    ).on(
      'txid -> transactionId.string,
      'index -> output.index,
      'value -> output.value,
      'address -> output.address.string,
      'tpos_owner_address -> output.tposOwnerAddress.map(_.string),
      'tpos_merchant_address -> output.tposMerchantAddress.map(_.string)
    ).as(parseTransactionOutput.singleOpt).flatten
  }

  private def deleteInputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Input] = {
    SQL(
      """
        |DELETE FROM transaction_inputs
        |WHERE txid = {txid}
        |RETURNING index, value, address
      """.stripMargin
    ).on(
      'txid -> txid.string
    ).as(parseTransactionInput.*)
  }

  private def deleteOutputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Output] = {
    val result = SQL(
      """
        |DELETE FROM transaction_outputs
        |WHERE txid = {txid}
        |RETURNING index, value, address, tpos_owner_address, tpos_merchant_address
      """.stripMargin
    ).on(
      'txid -> txid.string
    ).as(parseTransactionOutput.*)

    result.flatten
  }
}
