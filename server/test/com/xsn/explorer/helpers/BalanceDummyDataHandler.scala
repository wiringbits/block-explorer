package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.FieldOrdering
import com.alexitc.playsonify.models.pagination.{PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.BalanceBlockingDataHandler
import com.xsn.explorer.models.fields.BalanceField
import com.xsn.explorer.models.{Address, Balance}

class BalanceDummyDataHandler extends BalanceBlockingDataHandler {

  override def upsert(balance: Balance): ApplicationResult[Balance] = ???

  override def get(query: PaginatedQuery, ordering: FieldOrdering[BalanceField]): ApplicationResult[PaginatedResult[Balance]] = ???

  override def getBy(address: Address): ApplicationResult[Balance] = ???

  override def getNonZeroBalances(query: PaginatedQuery, ordering: FieldOrdering[BalanceField]): ApplicationResult[PaginatedResult[Balance]] = ???
}
