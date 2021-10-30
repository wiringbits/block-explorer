package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models.persisted.Balance

object BalanceParsers {

  import CommonParsers._

  val parseReceived = get[BigDecimal]("received")
  val parseSpent = get[BigDecimal]("spent")

  val parseBalance = (parseAddress() ~ parseReceived ~ parseSpent).map { case address ~ received ~ spent =>
    Balance(address, received, spent)
  }
}
