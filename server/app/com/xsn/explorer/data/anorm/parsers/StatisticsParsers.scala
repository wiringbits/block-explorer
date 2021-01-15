package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models.Statistics

object StatisticsParsers {

  val parseBlocks = int("blocks")
  val parseTransactions = int("transactions")
  val parseTotalSupply = get[BigDecimal]("total_supply")
  val parseCirculatingSupply = get[BigDecimal]("circulating_supply")

  val parseStatistics =
    (parseBlocks ~ parseTransactions ~ parseTotalSupply.? ~ parseCirculatingSupply.?)
      .map { case blocks ~ transactions ~ totalSupply ~ circulatingSupply =>
        Statistics(blocks, transactions, totalSupply, circulatingSupply)
      }
}
