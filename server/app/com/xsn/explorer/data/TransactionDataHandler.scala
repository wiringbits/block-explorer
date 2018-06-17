package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.{Blockhash, Transaction, TransactionId}

import scala.language.higherKinds

trait TransactionDataHandler[F[_]] {

  def upsert(transaction: Transaction): F[Transaction]

  def delete(transactionId: TransactionId): F[Transaction]

  def deleteBy(blockhash: Blockhash): F[List[Transaction]]
}

trait TransactionBlockingDataHandler extends TransactionDataHandler[ApplicationResult]
