package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models.{Address, Transaction, TransactionId}
import kamon.Kamon
import org.slf4j.LoggerFactory

class TransactionInputPostgresDAO {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def batchInsertInputs(
      inputs: List[(TransactionId, Transaction.Input)])(
      implicit conn: Connection): Option[List[(TransactionId, Transaction.Input)]] = {

    inputs match {
      case Nil => Some(inputs)

      case _ => timed("insert-input-batch") {
        val params = inputs.map { case (txid, input) =>
          List(
            'txid -> txid.string: NamedParameter,
            'index -> input.index: NamedParameter,
            'from_txid -> input.fromTxid.string: NamedParameter,
            'from_output_index -> input.fromOutputIndex: NamedParameter,
            'value -> input.value: NamedParameter,
            'address -> input.address.string: NamedParameter)
        }

        val batch = BatchSql(
          """
            |INSERT INTO transaction_inputs
            |  (txid, index, from_txid, from_output_index, value, address)
            |VALUES
            |  ({txid}, {index}, {from_txid}, {from_output_index}, {value}, {address})
          """.stripMargin,
          params.head,
          params.tail: _*
        )

        val success = batch.execute().forall(_ == 1)
        if (success) {
          Some(inputs)
        } else {
          None
        }
      }
    }
  }

  def deleteInputs(txid: TransactionId)(implicit conn: Connection): List[Transaction.Input] = {
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

  def getInputs(txid: TransactionId, address: Address)(implicit conn: Connection): List[Transaction.Input] = {
    SQL(
      """
        |SELECT txid, index, from_txid, from_output_index, value, address
        |FROM transaction_inputs
        |WHERE txid = {txid} AND
        |      address = {address}
      """.stripMargin
    ).on(
      'txid -> txid.string,
      'address -> address.string
    ).as(parseTransactionInput.*).flatten
  }

  private def timed[T](name: String)(f: => T): T = {
    val timer = Kamon.timer(name).start()
    try f
    finally timer.stop()
  }
}
