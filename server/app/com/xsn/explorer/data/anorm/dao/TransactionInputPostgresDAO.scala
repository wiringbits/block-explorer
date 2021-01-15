package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, TransactionId}
import javax.inject.Inject

class TransactionInputPostgresDAO @Inject() (explorerConfig: ExplorerConfig) {

  def batchInsertInputs(
      inputs: List[(TransactionId, Transaction.Input)]
  )(implicit
      conn: Connection
  ): Option[List[(TransactionId, Transaction.Input)]] = {

    inputs match {
      case Nil => Some(inputs)

      case _ =>
        val params = inputs.map { case (txid, input) =>
          List(
            'txid -> txid.toBytesBE.toArray: NamedParameter,
            'index -> input.index: NamedParameter,
            'from_txid -> input.fromTxid.toBytesBE.toArray: NamedParameter,
            'from_output_index -> input.fromOutputIndex: NamedParameter,
            'value -> input.value: NamedParameter,
            'addresses -> input.addresses.map(_.string).toArray: NamedParameter
          )
        }

        val batch = BatchSql(
          """
            |INSERT INTO transaction_inputs
            |  (txid, index, from_txid, from_output_index, value, addresses)
            |VALUES
            |  ({txid}, {index}, {from_txid}, {from_output_index}, {value}, {addresses})
          """.stripMargin,
          params.head,
          params.tail: _*
        )

        val result = batch.execute()
        val success = result.forall(_ == 1)
        if (
          success ||
          explorerConfig.liteVersionConfig.enabled
        ) {

          Some(inputs)
        } else {
          None
        }
    }
  }

  def upsert(txid: TransactionId, input: Transaction.Input)(implicit
      conn: Connection
  ): Unit = {
    val _ = SQL(
      """
        |INSERT INTO transaction_inputs
        |  (txid, index, from_txid, from_output_index, value, addresses)
        |VALUES
        |  ({txid}, {index}, {from_txid}, {from_output_index}, {value}, {addresses})
        |ON CONFLICT (txid, index) DO UPDATE
        |SET txid = EXCLUDED.txid,
        |    index = EXCLUDED.index,
        |    from_txid = EXCLUDED.from_txid,
        |    from_output_index = EXCLUDED.from_output_index,
        |    value = EXCLUDED.value,
        |    addresses = EXCLUDED.addresses
      """.stripMargin
    ).on(
      'txid -> txid.toBytesBE.toArray,
      'index -> input.index,
      'from_txid -> input.fromTxid.toBytesBE.toArray,
      'from_output_index -> input.fromOutputIndex,
      'value -> input.value,
      'addresses -> input.addresses.map(_.string).toArray
    ).execute()
  }

  def deleteInputs(
      txid: TransactionId
  )(implicit conn: Connection): List[Transaction.Input] = {
    SQL(
      """
        |DELETE FROM transaction_inputs
        |WHERE txid = {txid}
        |RETURNING txid, index, from_txid, from_output_index, value, addresses
      """.stripMargin
    ).on(
      'txid -> txid.toBytesBE.toArray
    ).as(parseTransactionInput.*)
  }

  def getInputs(
      txid: TransactionId
  )(implicit conn: Connection): List[Transaction.Input] = {
    SQL(
      """
        |SELECT txid, index, from_txid, from_output_index, value, addresses
        |FROM transaction_inputs
        |WHERE txid = {txid}
        |ORDER BY index
      """.stripMargin
    ).on(
      'txid -> txid.toBytesBE.toArray
    ).as(parseTransactionInput.*)
  }

  def getInputs(txid: TransactionId, address: Address)(implicit
      conn: Connection
  ): List[Transaction.Input] = {
    SQL(
      """
        |SELECT txid, index, from_txid, from_output_index, value, addresses
        |FROM transaction_inputs
        |WHERE txid = {txid} AND
        |      {address} = ANY(addresses)
        |ORDER BY index
      """.stripMargin
    ).on(
      'txid -> txid.toBytesBE.toArray,
      'address -> address.string
    ).as(parseTransactionInput.*)
  }
}
