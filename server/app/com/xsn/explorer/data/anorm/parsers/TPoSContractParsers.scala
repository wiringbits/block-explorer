package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models.TPoSContract

object TPoSContractParsers {

  import CommonParsers._

  val parseOwner = parseAddress("owner")
  val parseMerchant = parseAddress("merchant")

  val parseMerchantCommission = int("merchant_commission")
    .map(TPoSContract.Commission.from)
    .map { _.getOrElse(throw new RuntimeException("corrupted merchant_commission")) }

  val parseTPoSContractState = str("state")
    .map(TPoSContract.State.withNameInsensitiveOption)
    .map { _.getOrElse(throw new RuntimeException("corrupted state")) }

  val parseTPoSContract = (parseTransactionId() ~
    parseIndex ~
    parseOwner ~
    parseMerchant ~
    parseMerchantCommission ~
    parseTime ~
    parseTPoSContractState).map {

    case txid ~ index ~ owner ~ merchant ~ merchantCommission ~ time ~ state =>
      val details = TPoSContract.Details(owner = owner, merchant = merchant, merchantCommission = merchantCommission)
      TPoSContract(TPoSContract.Id(txid, index), details, time, state)
  }
}
