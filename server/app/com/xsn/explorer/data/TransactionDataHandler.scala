package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField

import scala.language.higherKinds

trait TransactionDataHandler[F[_]] {

  def getBy(
      address: Address,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): F[PaginatedResult[TransactionWithValues]]

  def getUnspentOutputs(address: Address): F[List[Transaction.Output]]

  def getByBlockhash(
      blockhash: Blockhash,
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[TransactionField]): F[PaginatedResult[TransactionWithValues]]

  def getLatestTransactionBy(addresses: List[Address]): F[Map[String, String]]
}

trait TransactionBlockingDataHandler extends TransactionDataHandler[ApplicationResult]
