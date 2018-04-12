package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.base.{PaginatedQuery, PaginatedResult}

import scala.language.higherKinds

trait BalanceDataHandler[F[_]] {

  def upsert(balance: Balance): F[Balance]

  def getRichest(query: PaginatedQuery): F[PaginatedResult[Balance]]

  def getCirculatingSupply(): F[BigDecimal]
}

trait BalanceBlockingDataHandler extends BalanceDataHandler[ApplicationResult]
