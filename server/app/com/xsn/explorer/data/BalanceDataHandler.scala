package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.fields.BalanceField

import scala.language.higherKinds

trait BalanceDataHandler[F[_]] {

  def upsert(balance: Balance): F[Balance]

  def get(query: PaginatedQuery, ordering: FieldOrdering[BalanceField]): F[PaginatedResult[Balance]]

  def getNonZeroBalances(query: PaginatedQuery, ordering: FieldOrdering[BalanceField]): F[PaginatedResult[Balance]]
}

trait BalanceBlockingDataHandler extends BalanceDataHandler[ApplicationResult]
