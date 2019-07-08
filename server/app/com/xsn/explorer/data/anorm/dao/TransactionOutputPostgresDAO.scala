package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, TransactionId}
import javax.inject.Inject
import org.slf4j.LoggerFactory

class TransactionOutputPostgresDAO @Inject()(explorerConfig: ExplorerConfig) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getUnspentOutputs(address: Address)(implicit conn: Connection): List[Transaction.Output] = {
    SQL(
      """
        |SELECT txid, index, value, addresses, hex_script
        |FROM transaction_outputs
        |WHERE {address} = ANY(addresses) AND
        |      spent_on IS NULL AND
        |      value > 0
      """.stripMargin
    ).on(
        'address -> address.string
      )
      .as(parseTransactionOutput.*)
  }

  def getOutput(txid: TransactionId, index: Int)(implicit conn: Connection): Option[Transaction.Output] = {
    SQL(
      """
        |SELECT txid, index, value, addresses, hex_script
        |FROM transaction_outputs
        |WHERE txid = {txid} AND
        |      index = {index}
      """.stripMargin
    ).on(
        'txid -> txid.string,
        'index -> index
      )
      .as(parseTransactionOutput.singleOpt)
  }

  def batchInsertOutputs(
      outputs: List[Transaction.Output]
  )(implicit conn: Connection): Option[List[Transaction.Output]] = {

    outputs match {
      case Nil => Some(outputs)
      case _ =>
        val params = outputs.map { output =>
          List(
            'txid -> output.txid.string: NamedParameter,
            'index -> output.index: NamedParameter,
            'value -> output.value: NamedParameter,
            'addresses -> output.addresses.map(_.string).toArray: NamedParameter,
            'hex_script -> output.script.string: NamedParameter
          )
        }

        val batch = BatchSql(
          """
            |INSERT INTO transaction_outputs
            |  (txid, index, value, addresses, hex_script)
            |VALUES
            |  ({txid}, {index}, {value}, {addresses}, {hex_script})
          """.stripMargin,
          params.head,
          params.tail: _*
        )

        val success = batch.execute().forall(_ == 1)
        if (success ||
          explorerConfig.liteVersionConfig.enabled) {

          Some(outputs)
        } else {
          None
        }
    }
  }

  def upsert(output: Transaction.Output)(implicit conn: Connection): Unit = {
    val _ = SQL("""
        |INSERT INTO transaction_outputs
        |  (txid, index, value, addresses, hex_script)
        |VALUES
        |  ({txid}, {index}, {value}, {addresses}, {hex_script})
        |ON CONFLICT (txid, index) DO UPDATE
        |SET txid = EXCLUDED.txid,
        |    index = EXCLUDED.index,
        |    value = EXCLUDED.value,
        |    addresses = EXCLUDED.addresses,
        |    hex_script = EXCLUDED.hex_script
      """.stripMargin)
      .on(
        'txid -> output.txid.string,
        'index -> output.index,
        'value -> output.value,
        'addresses -> output.addresses.map(_.string).toArray,
        'hex_script -> output.script.string
      )
      .execute()
  }

  def deleteOutputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Output] = {
    val result = SQL(
      """
        |DELETE FROM transaction_outputs
        |WHERE txid = {txid}
        |RETURNING txid, index, hex_script, value, addresses
      """.stripMargin
    ).on(
        'txid -> txid.string
      )
      .as(parseTransactionOutput.*)

    result
  }

  def getOutputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Output] = {
    SQL(
      """
        |SELECT txid, index, hex_script, value, addresses
        |FROM transaction_outputs
        |WHERE txid = {txid}
        |ORDER BY index
      """.stripMargin
    ).on(
        'txid -> txid.string
      )
      .as(parseTransactionOutput.*)
  }

  def getOutputs(txid: TransactionId, address: Address)(implicit conn: Connection): List[Transaction.Output] = {
    SQL(
      """
        |SELECT txid, index, hex_script, value, addresses
        |FROM transaction_outputs
        |WHERE txid = {txid} AND
        |      {address} = ANY(addresses)
        |ORDER BY index
      """.stripMargin
    ).on(
        'txid -> txid.string,
        'address -> address.string
      )
      .as(parseTransactionOutput.*)
  }

  def batchSpend(txid: TransactionId, inputs: List[Transaction.Input])(implicit conn: Connection): Option[Unit] = {
    inputs match {
      case Nil => Option(())
      case _ =>
        val txidArray = inputs
          .map { input =>
            s"'${input.fromTxid.string}'"
          }
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

        if (result == inputs.size ||
          explorerConfig.liteVersionConfig.enabled) {

          Option(())
        } else {
          None
        }
    }
  }

  def spend(txid: TransactionId, index: Int, spentOn: TransactionId)(implicit conn: Connection): Unit = {
    val _ = SQL(
      s"""
         |UPDATE transaction_outputs
         |SET spent_on = {spent_on}
         |WHERE txid = {txid} AND
         |      index = {index}
        """.stripMargin
    ).on("txid" -> txid.string, "index" -> index, "spent_on" -> spentOn.string)
      .executeUpdate()
  }
}
