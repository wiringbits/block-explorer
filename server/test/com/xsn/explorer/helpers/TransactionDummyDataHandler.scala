package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import org.scalactic.Every

class TransactionDummyDataHandler extends TransactionBlockingDataHandler {

  override def getBy(address: Address, paginatedQuery: PaginatedQuery, ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = ???

  override def getUnspentOutputs(address: Address): ApplicationResult[List[Transaction.Output]] = ???

  override def getByBlockhash(blockhash: Blockhash, paginatedQuery: PaginatedQuery, ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = ???

  override def getLatestTransactionBy(addresses: Every[Address]): ApplicationResult[Map[String, String]] = ???
}
