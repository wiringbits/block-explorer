package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.{PaginatedQuery, PaginatedResult}
import com.xsn.explorer.models._

import scala.language.higherKinds

trait TransactionDataHandler[F[_]] {

  def upsert(transaction: Transaction): F[Transaction]

  def delete(transactionId: TransactionId): F[Transaction]

  def deleteBy(blockhash: Blockhash): F[List[Transaction]]

  def getBy(address: Address, paginatedQuery: PaginatedQuery): F[PaginatedResult[TransactionWithValues]]
}

trait TransactionBlockingDataHandler extends TransactionDataHandler[ApplicationResult]
