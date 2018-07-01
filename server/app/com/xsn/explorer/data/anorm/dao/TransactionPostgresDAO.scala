package com.xsn.explorer.data.anorm.dao

import java.sql.Connection
import javax.inject.Inject

import anorm._
import com.alexitc.playsonify.models.{Count, FieldOrdering, PaginatedQuery}
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField

class TransactionPostgresDAO @Inject() (fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter) {

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

  def getBy(
      address: Address,
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
        |SELECT t.txid, blockhash, time, size,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_inputs WHERE txid = t.txid AND address = {address}) AS sent,
        |       (SELECT COALESCE(SUM(value), 0) FROM transaction_outputs WHERE txid = t.txid AND address = {address}) AS received
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
}
