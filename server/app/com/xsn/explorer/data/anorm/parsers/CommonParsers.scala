package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser.{int, long, str}
import com.xsn.explorer.models.values._

object CommonParsers {

  def parseBlockhash(field: String = "blockhash") = str(field)
      .map(Blockhash.from)
      .map { _.getOrElse(throw new RuntimeException(s"corrupted $field")) }

  def parseAddress(field: String = "address") = str(field)
      .map(Address.from)
      .map { _.getOrElse(throw new RuntimeException(s"corrupted $field")) }

  def parseTransactionId(field: String = "txid") = str(field)
      .map(TransactionId.from)
      .map { _.getOrElse(throw new RuntimeException(s"corrupted $field")) }

  def parseHexString(field: String) = str(field)
      .map(HexString.from)
      .map { _.getOrElse(throw new RuntimeException(s"corrupted $field")) }

  val parseTime = long("time")
  val parseSize = int("size").map(Size.apply)
  val parseIndex = int("index")
}
