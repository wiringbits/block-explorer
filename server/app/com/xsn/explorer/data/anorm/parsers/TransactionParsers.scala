package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser.{get, str}
import anorm.~
import com.xsn.explorer.models.{Address, Transaction, TransactionId}

object TransactionParsers {

  import CommonParsers._

  val parseTransactionId = str("txid").map(TransactionId.from)
  val parseReceived = get[BigDecimal]("received")
  val parseSpent = get[BigDecimal]("spent")

  val parseIndex = get[Int]("index")
  val parseValue = get[BigDecimal]("value")

  val parseTposOwnerAddress = str("tpos_owner_address").map(Address.from)
  val parseTposMerchantAddress = str("tpos_merchant_address").map(Address.from)

  val parseTransaction = (parseTransactionId ~ parseBlockhash ~ parseTime ~ parseSize).map {
    case txidMaybe ~ blockhashMaybe ~ time ~ size =>
      for {
        txid <- txidMaybe
        blockhash <- blockhashMaybe
      } yield Transaction(txid, blockhash, time, size, List.empty, List.empty)
  }

  val parseTransactionInput = (parseIndex ~ parseValue.? ~ parseAddress.?).map { case index ~ value ~ address =>
    Transaction.Input(index, value, address.flatten)
  }

  val parseTransactionOutput = (
      parseIndex ~
          parseValue ~
          parseAddress ~
          parseTposOwnerAddress.? ~
          parseTposMerchantAddress.?).map {

    case index ~ value ~ addressMaybe ~ tposOwnerAddress ~ tposMerchantAddress =>
      for (address <- addressMaybe)
        yield Transaction.Output(index, value, address, tposOwnerAddress.flatten, tposMerchantAddress.flatten)
  }
}
