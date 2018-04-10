package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser.{int, long, str}
import com.xsn.explorer.models.{Address, Blockhash, Size}

object CommonParsers {

  val parseBlockhash = str("blockhash").map(Blockhash.from)
  val parseAddress = str("address").map(Address.from)
  val parseTime = long("time")
  val parseSize = int("size").map(Size.apply)
}
