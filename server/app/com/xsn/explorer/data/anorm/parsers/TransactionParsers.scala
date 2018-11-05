package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser.{get, str}
import anorm.~
import com.xsn.explorer.models._

object TransactionParsers {

  import CommonParsers._

  val parseTransactionId = str("txid").map(TransactionId.from)
  val parseFromTxid = str("from_txid").map(TransactionId.from)
  val parseFromOutputIndex = get[Int]("from_output_index")
  val parseReceived = get[BigDecimal]("received")
  val parseSpent = get[BigDecimal]("spent")
  val parseSent = get[BigDecimal]("sent")

  val parseIndex = get[Int]("index")
  val parseValue = get[BigDecimal]("value")
  val parseHexString = get[String]("hex_script").map(HexString.from)

  val parseTposOwnerAddress = str("tpos_owner_address").map(Address.from)
  val parseTposMerchantAddress = str("tpos_merchant_address").map(Address.from)

  val parseTransaction = (parseTransactionId ~ parseBlockhash ~ parseTime ~ parseSize).map {
    case txidMaybe ~ blockhashMaybe ~ time ~ size =>
      for {
        txid <- txidMaybe
        blockhash <- blockhashMaybe
      } yield Transaction(txid, blockhash, time, size, List.empty, List.empty)
  }

  val parseTransactionWithValues = (
      parseTransactionId ~
          parseBlockhash ~
          parseTime ~
          parseSize ~
          parseSent ~
          parseReceived).map {

    case txidMaybe ~ blockhashMaybe ~ time ~ size ~ sent ~ received =>
      for {
        txid <- txidMaybe
        blockhash <- blockhashMaybe
      } yield TransactionWithValues(txid, blockhash, time, size, sent, received)
  }

  val parseTransactionInput = (parseFromTxid ~ parseFromOutputIndex ~ parseIndex ~ parseValue ~ parseAddress)
      .map { case fromTxidMaybe ~ fromOutputIndex ~ index ~ value ~ addressMaybe =>
        for {
          from <- fromTxidMaybe
          address <- addressMaybe
        } yield Transaction.Input(from, fromOutputIndex, index, value, address)
      }

  val parseTransactionOutput = (
      parseTransactionId ~
          parseIndex ~
          parseValue ~
          parseAddress ~
          parseHexString ~
          parseTposOwnerAddress.? ~
          parseTposMerchantAddress.?).map {

    case txidMaybe ~ index ~ value ~ addressMaybe ~ scriptMaybe ~ tposOwnerAddress ~ tposMerchantAddress =>
      for {
        txid <- txidMaybe
        address <- addressMaybe
        script <- scriptMaybe
      } yield Transaction.Output(txid, index, value, address, script, tposOwnerAddress.flatten, tposMerchantAddress.flatten)
  }

  val parseAddressTransactionDetails = (parseAddress ~ parseTransactionId ~ parseSent ~ parseReceived ~ parseTime).map {
    case address ~ txid ~ sent ~ received ~ time => AddressTransactionDetails(
      address.getOrElse(throw new RuntimeException("failed to retrieve address")),
      txid.getOrElse(throw new RuntimeException("failed to retrieve txid")),
      time = time,
      sent = sent,
      received = received)
  }
}
