package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser.{get, str}
import anorm.~
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.{AddressTransactionDetails, Transaction}
import com.xsn.explorer.models.values._

object TransactionParsers {

  import CommonParsers._

  val parseTransactionId = str("txid")
      .map(TransactionId.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted txid")) }

  val parseFromTxid = str("from_txid")
      .map(TransactionId.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted from_txid")) }

  val parseFromOutputIndex = get[Int]("from_output_index")
  val parseReceived = get[BigDecimal]("received")
  val parseSpent = get[BigDecimal]("spent")
  val parseSent = get[BigDecimal]("sent")

  val parseIndex = get[Int]("index")
  val parseValue = get[BigDecimal]("value")
  val parseHexString = get[String]("hex_script")
      .map(HexString.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted hex_script")) }

  val parseTposOwnerAddress = str("tpos_owner_address")
      .map(Address.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted tpos_owner_address")) }

  val parseTposMerchantAddress = str("tpos_merchant_address")
      .map(Address.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted tpos_merchant_address")) }

  val parseTransaction = (parseTransactionId ~ parseBlockhash ~ parseTime ~ parseSize).map {
    case txid ~ blockhash ~ time ~ size => Transaction(txid, blockhash, time, size)
  }

  val parseTransactionWithValues = (
      parseTransactionId ~
          parseBlockhash ~
          parseTime ~
          parseSize ~
          parseSent ~
          parseReceived).map {

    case txid ~ blockhash ~ time ~ size ~ sent ~ received =>
      TransactionWithValues(txid, blockhash, time, size, sent, received)
  }

  val parseTransactionInput = (parseFromTxid ~ parseFromOutputIndex ~ parseIndex ~ parseValue ~ parseAddress)
      .map { case fromTxid ~ fromOutputIndex ~ index ~ value ~ address =>
        Transaction.Input(fromTxid, fromOutputIndex, index, value, address)
      }

  val parseTransactionOutput = (
      parseTransactionId ~
          parseIndex ~
          parseValue ~
          parseAddress ~
          parseHexString ~
          parseTposOwnerAddress.? ~
          parseTposMerchantAddress.?).map {

    case txid ~ index ~ value ~ address ~ script ~ tposOwnerAddress ~ tposMerchantAddress =>
      Transaction.Output(txid, index, value, address, script, tposOwnerAddress, tposMerchantAddress)
  }

  val parseAddressTransactionDetails = (parseAddress ~ parseTransactionId ~ parseSent ~ parseReceived ~ parseTime).map {
    case address ~ txid ~ sent ~ received ~ time => AddressTransactionDetails(
      address,
      txid,
      time = time,
      sent = sent,
      received = received)
  }
}
