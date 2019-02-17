package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser.{int, long, str}
import com.xsn.explorer.models.values._
import com.xsn.explorer.models.Size

object CommonParsers {

  val parseBlockhash = str("blockhash")
      .map(Blockhash.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted blockhash")) }

  val parseAddress = str("address").map(Address.from)
  val parseTime = long("time")
  val parseSize = int("size").map(Size.apply)
}
