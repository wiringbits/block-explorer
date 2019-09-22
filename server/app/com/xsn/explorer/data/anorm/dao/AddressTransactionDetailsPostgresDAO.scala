package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.data.anorm.parsers.TransactionParsers._
import com.xsn.explorer.models.persisted.{AddressTransactionDetails, Transaction}
import com.xsn.explorer.models.values.TransactionId
import com.xsn.explorer.util.TransactionAddressesHelper
import javax.inject.Inject
import org.slf4j.LoggerFactory

class AddressTransactionDetailsPostgresDAO @Inject()(explorerConfig: ExplorerConfig) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def batchInsertDetails(transaction: Transaction.HasIO)(implicit conn: Connection): Option[Unit] = {
    val details = TransactionAddressesHelper.computeDetails(transaction)
    batchInsertDetails(details.toList)
  }

  def batchInsertDetails(details: List[AddressTransactionDetails])(implicit conn: Connection): Option[Unit] = {
    details match {
      case Nil => Some(())
      case _ =>
        val params = details.map { d =>
          List(
            'address -> d.address.string: NamedParameter,
            'txid -> d.txid.toBytesBE.toArray: NamedParameter,
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

  def upsert(details: AddressTransactionDetails)(implicit conn: Connection): Unit = {
    val _ = SQL(
      """
        |INSERT INTO address_transaction_details
        |  (address, txid, received, sent, time)
        |VALUES
        |  ({address}, {txid}, {received}, {sent}, {time})
        |ON CONFLICT (address, txid) DO UPDATE
        |SET address = EXCLUDED.address,
        |    txid = EXCLUDED.txid,
        |    received = EXCLUDED.received,
        |    sent = EXCLUDED.sent,
        |    time = EXCLUDED.time
      """.stripMargin
    ).on(
        'address -> details.address.string,
        'txid -> details.txid.toBytesBE.toArray,
        'received -> details.received,
        'sent -> details.sent,
        'time -> details.time
      )
      .executeUpdate()
  }

  def deleteDetails(txid: TransactionId)(implicit conn: Connection): List[AddressTransactionDetails] = {
    val result = SQL(
      """
        |DELETE FROM address_transaction_details
        |WHERE txid = {txid}
        |RETURNING address, txid, received, sent, time
      """.stripMargin
    ).on(
        'txid -> txid.toBytesBE.toArray
      )
      .as(parseAddressTransactionDetails.*)

    result
  }
}
