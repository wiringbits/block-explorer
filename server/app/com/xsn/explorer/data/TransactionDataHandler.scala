package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.{Address, Blockhash, TransactionId}

import scala.language.higherKinds

trait TransactionDataHandler[F[_]] {

  def getBy(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): F[List[TransactionInfo.HasIO]]

  def getByAddress(
      address: Address,
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition
  ): F[List[TransactionInfo]]

  def getUnspentOutputs(address: Address): F[List[Transaction.Output]]

  def getOutput(txid: TransactionId, index: Int): F[Transaction.Output]

  def get(
      limit: Limit,
      lastSeenTxid: Option[TransactionId],
      orderingCondition: OrderingCondition,
      includeZeroValueTransactions: Boolean
  ): F[List[TransactionInfo]]

  def getByBlockhash(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]
  ): F[List[TransactionWithValues]]

  def getTransactionsWithIOBy(
      blockhash: Blockhash,
      limit: Limit,
      lastSeenTxid: Option[TransactionId]
  ): F[List[Transaction.HasIO]]
}

trait TransactionBlockingDataHandler extends TransactionDataHandler[ApplicationResult]
