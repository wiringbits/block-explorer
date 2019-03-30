package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm.~
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.{AddressTransactionDetails, Transaction}

object TransactionParsers {

  import CommonParsers._

  val parseFromTxid = parseTransactionId("from_txid")
  val parseFromOutputIndex = get[Int]("from_output_index")
  val parseReceived = get[BigDecimal]("received")
  val parseSpent = get[BigDecimal]("spent")
  val parseSent = get[BigDecimal]("sent")
  val parseValue = get[BigDecimal]("value")
  val parseHexScript = parseHexString("hex_script")

  val parseTposOwnerAddress = parseAddress("tpos_owner_address")
  val parseTposMerchantAddress = parseAddress("tpos_merchant_address")

  val parseTransaction = (parseTransactionId() ~ parseBlockhash() ~ parseTime ~ parseSize).map {
    case txid ~ blockhash ~ time ~ size => Transaction(txid, blockhash, time, size)
  }

  val parseTransactionWithValues = (
      parseTransactionId() ~
          parseBlockhash() ~
          parseTime ~
          parseSize ~
          parseSent ~
          parseReceived).map {

    case txid ~ blockhash ~ time ~ size ~ sent ~ received =>
      TransactionWithValues(txid, blockhash, time, size, sent, received)
  }

  val parseTransactionInput = (parseFromTxid ~ parseFromOutputIndex ~ parseIndex ~ parseValue ~ parseAddress())
      .map { case fromTxid ~ fromOutputIndex ~ index ~ value ~ address =>
        Transaction.Input(fromTxid, fromOutputIndex, index, value, address)
      }

  val parseTransactionOutput = (
      parseTransactionId() ~
          parseIndex ~
          parseValue ~
          parseAddress() ~
          parseHexScript ~
          parseTposOwnerAddress.? ~
          parseTposMerchantAddress.?).map {

    case txid ~ index ~ value ~ address ~ script ~ tposOwnerAddress ~ tposMerchantAddress =>
      Transaction.Output(txid, index, value, address, script, tposOwnerAddress, tposMerchantAddress)
  }

  val parseAddressTransactionDetails = (parseAddress() ~ parseTransactionId() ~ parseSent ~ parseReceived ~ parseTime).map {
    case address ~ txid ~ sent ~ received ~ time => AddressTransactionDetails(
      address,
      txid,
      time = time,
      sent = sent,
      received = received)
  }
}
