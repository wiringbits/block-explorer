package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination
import com.alexitc.playsonify.models.pagination.{PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField

class TransactionDummyDataHandler extends TransactionBlockingDataHandler {

  override def getBy(address: Address, paginatedQuery: PaginatedQuery, ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = ???

  override def getBy(address: Address, limit: pagination.Limit, lastSeenTxid: Option[TransactionId], orderingCondition: OrderingCondition): ApplicationResult[List[Transaction]] = ???

  override def getUnspentOutputs(address: Address): ApplicationResult[List[Transaction.Output]] = ???

  override def getByBlockhash(blockhash: Blockhash, paginatedQuery: PaginatedQuery, ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = ???

  override def getByBlockhash(blockhash: Blockhash, limit: pagination.Limit, lastSeenTxid: Option[TransactionId]): ApplicationResult[List[TransactionWithValues]] = ???

}
