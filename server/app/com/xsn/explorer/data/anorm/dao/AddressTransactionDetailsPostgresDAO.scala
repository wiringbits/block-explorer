package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models.persisted.{AddressTransactionDetails, Transaction}
import com.xsn.explorer.models.values.TransactionId

class AddressTransactionDetailsPostgresDAO {

  def batchInsertDetails(transaction: Transaction)(implicit conn: Connection): Option[Unit] = {
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

    val details = (received ++ sent)
        .groupBy(_.address)
        .mapValues {
          case head :: list => list.foldLeft(head) { (acc, current) =>
            current.copy(received = current.received + acc.received, sent = current.sent + acc.sent)
          }
        }
        .values

    batchInsertDetails(details.toList)
  }

  def batchInsertDetails(details: List[AddressTransactionDetails])(implicit conn: Connection): Option[Unit] = {
    details match {
      case Nil => Some(())
      case _ =>
        val params = details.map { d =>
          List(
            'address  -> d.address.string: NamedParameter,
            'txid -> d.txid.string: NamedParameter,
            'received -> d.received: NamedParameter,
            'sent -> d.sent: NamedParameter,
            'time -> d.time: NamedParameter)
        }

        val batch = BatchSql(
          """
            |INSERT INTO address_transaction_details
            |  (address, txid, received, sent, time)
            |VALUES
            |  ({address}, {txid}, {received}, {sent}, {time})
          """.stripMargin,
          params.head,
          params.tail: _*
        )

        val success = batch.execute().forall(_ == 1)

        if (success) {
          Some(())
        } else {
          None
        }
    }
  }

  def deleteDetails(txid: TransactionId)(implicit conn: Connection): List[AddressTransactionDetails] = {
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
}
