package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.values.HexString

object BlockFilterParsers {

  val parseP = int("p")
  val parseM = int("m")
  val parseN = int("n")
  val parseHex = str("hex")
      .map(HexString.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted hex")) }

  val parseFilter = (parseN ~ parseM ~ parseP ~ parseHex).map {
    case n ~ m ~ p ~ hex =>
      new GolombCodedSet(
        n = n,
        m = m,
        p = p,
        hex = hex
      )
  }
}
