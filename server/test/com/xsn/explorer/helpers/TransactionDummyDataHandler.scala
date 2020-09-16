package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.models.pagination
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, Blockhash, TransactionId}

class TransactionDummyDataHandler extends TransactionBlockingDataHandler {

  override def getBy(
      address: Address,
      limit: pagination.Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): ApplicationResult[List[Transaction.HasIO]] = ???

  override def getByAddress(
      address: Address,
      limit: pagination.Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): ApplicationResult[List[TransactionInfo]] = ???

  override def getUnspentOutputs(address: Address): ApplicationResult[List[Transaction.Output]] = ???

  override def getOutput(txid: TransactionId, index: Int): ApplicationResult[Transaction.Output] = ???

  override def get(
      limit: pagination.Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): ApplicationResult[List[TransactionInfo]] = ???

  override def getByBlockhash(
      blockhash: Blockhash,
      limit: pagination.Limit,
      lastSeenTxid: Option[TransactionId]
  ): ApplicationResult[List[TransactionWithValues]] = ???

  override def getTransactionsWithIOBy(
      blockhash: Blockhash,
      limit: pagination.Limit,
      lastSeenTxid: Option[TransactionId]
  ): ApplicationResult[List[Transaction.HasIO]] = ???
}
