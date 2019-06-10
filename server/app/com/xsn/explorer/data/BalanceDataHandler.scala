package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.FieldOrdering
import com.alexitc.playsonify.models.pagination.{Limit, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.models.fields.BalanceField
import com.xsn.explorer.models.persisted.Balance
import com.xsn.explorer.models.values.Address

import scala.language.higherKinds

trait BalanceDataHandler[F[_]] {

  def upsert(balance: Balance): F[Balance]

  def get(query: PaginatedQuery, ordering: FieldOrdering[BalanceField]): F[PaginatedResult[Balance]]

  def getBy(address: Address): F[Balance]

  def getHighestBalances(limit: Limit, lastSeenAddress: Option[Address]): F[List[Balance]]
}

trait BalanceBlockingDataHandler extends BalanceDataHandler[ApplicationResult]
