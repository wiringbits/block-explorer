package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models.persisted.{AddressTransactionDetails, Transaction}
import com.xsn.explorer.models.values.TransactionId
import javax.inject.Inject
import org.slf4j.LoggerFactory

class AddressTransactionDetailsPostgresDAO @Inject()(explorerConfig: ExplorerConfig) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def batchInsertDetails(transaction: Transaction.HasIO)(implicit conn: Connection): Option[Unit] = {
    val outputAddressValueList = for {
      output <- transaction.outputs
      address <- output.addresses
    } yield address -> output.value

    val received = outputAddressValueList
      .groupBy(_._1)
      .mapValues { _.map(_._2).sum }
      .map {
        case (address, value) =>
          AddressTransactionDetails(address, transaction.id, time = transaction.time, received = value)
      }

    val inputAddressValueList = for {
      input <- transaction.inputs
      address <- input.addresses
    } yield address -> input.value

    val sent = inputAddressValueList
      .groupBy(_._1)
      .mapValues { _.map(_._2).sum }
      .map {
        case (address, value) =>
          AddressTransactionDetails(address, transaction.id, time = transaction.time, sent = value)
      }

    val details = (received ++ sent)
      .groupBy(_.address)
      .mapValues {
        case head :: list =>
          list.foldLeft(head) { (acc, current) =>
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
            'address -> d.address.string: NamedParameter,
            'txid -> d.txid.string: NamedParameter,
            'received -> d.received: NamedParameter,
            'sent -> d.sent: NamedParameter,
            'time -> d.time: NamedParameter
          )
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

        val result = batch.execute()
        val success = result.forall(_ == 1)

        if (success ||
          explorerConfig.liteVersionConfig.enabled) {

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
      )
      .as(parseAddressTransactionDetails.*)

    result
  }
}
