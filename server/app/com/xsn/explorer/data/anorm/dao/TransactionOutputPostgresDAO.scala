package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import org.slf4j.LoggerFactory

class TransactionOutputPostgresDAO {

  private val logger = LoggerFactory.getLogger(this.getClass)

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

  def batchInsertOutputs(
      outputs: List[Transaction.Output])(
      implicit conn: Connection): Option[List[Transaction.Output]] = {

    outputs match {
      case Nil => Some(outputs)
      case _ =>
        val params = outputs.map { output =>
          List(
            'txid -> output.txid.string: NamedParameter,
            'index -> output.index: NamedParameter,
            'value -> output.value: NamedParameter,
            'address -> output.address.string: NamedParameter,
            'hex_script -> output.script.string: NamedParameter,
            'tpos_owner_address -> output.tposOwnerAddress.map(_.string): NamedParameter,
            'tpos_merchant_address -> output.tposMerchantAddress.map(_.string): NamedParameter)
        }

        val batch = BatchSql(
          """
            |INSERT INTO transaction_outputs
            |  (txid, index, value, address, hex_script, tpos_owner_address, tpos_merchant_address)
            |VALUES
            |  ({txid}, {index}, {value}, {address}, {hex_script}, {tpos_owner_address}, {tpos_merchant_address})
          """.stripMargin,
          params.head,
          params.tail: _*
        )

        val success = batch.execute().forall(_ == 1)
        if (success) {
          Some(outputs)
        } else {
          None
        }
    }
  }

  def deleteOutputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Output] = {
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

  def getOutputs(txid: TransactionId, address: Address)(implicit conn: Connection): List[Transaction.Output] = {
    SQL(
      """
        |SELECT txid, index, hex_script, value, address, tpos_owner_address, tpos_merchant_address
        |FROM transaction_outputs
        |WHERE txid = {txid} AND
        |      address = {address}
      """.stripMargin
    ).on(
      'txid -> txid.string,
      'address -> address.string
    ).as(parseTransactionOutput.*).flatten
  }

  def batchSpend(txid: TransactionId, inputs: List[Transaction.Input])(implicit conn: Connection): Option[Unit] = {
    inputs match {
      case Nil => Option(())
      case _ =>
        val txidArray = inputs
            .map { input => s"'${input.fromTxid.string}'" }
            .mkString("[", ",", "]")

        val indexArray = inputs.map(_.fromOutputIndex).mkString("[", ",", "]")

        // Note: the TransactionId must meet a safe format, this approach speeds up the inserts
        val result = SQL(
          s"""
             |UPDATE transaction_outputs t
             |SET spent_on = tmp.spent_on
             |FROM (
             |  WITH CTE AS (
             |    SELECT '${txid.string}' AS spent_on
             |  )
             |  SELECT spent_on, txid, index
             |  FROM CTE CROSS JOIN (SELECT
             |       UNNEST(array$indexArray) AS index,
             |       UNNEST(array$txidArray) AS txid) x
             |) AS tmp
             |WHERE t.txid = tmp.txid AND
             |      t.index = tmp.index
        """.stripMargin
        ).executeUpdate()

        if (result == inputs.size) {
          Option(())
        } else {
          None
        }
    }
  }
}
