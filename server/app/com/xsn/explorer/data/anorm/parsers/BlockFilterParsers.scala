package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.values.CompactSizeInt

object BlockFilterParsers {

  import CommonParsers._

  val parseP = int("p")
  val parseM = int("m")
  val parseN = int("n")
  val parseHex = parseHexString("hex")

  val parseFilter = (parseN ~ parseM ~ parseP ~ parseHex).map {
    case n ~ m ~ p ~ hex =>
      val compactN = CompactSizeInt(n)
      new GolombCodedSet(
        n = n,
        m = m,
        p = p,
        hex = hex.drop(compactN.hex.length)
      )
  }
}
