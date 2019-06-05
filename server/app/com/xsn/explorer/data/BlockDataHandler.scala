package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Limit, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.persisted.{Block, BlockHeader}
import com.xsn.explorer.models.values.{Blockhash, Height}

import scala.language.higherKinds

trait BlockDataHandler[F[_]] {

  def getBy(blockhash: Blockhash): F[Block]

  def getBy(height: Height): F[Block]

  def getBy(paginatedQuery: PaginatedQuery, ordering: FieldOrdering[BlockField]): F[PaginatedResult[Block]]

  def delete(blockhash: Blockhash): F[Block]

  def getLatestBlock(): F[Block]

  def getFirstBlock(): F[Block]

  def getHeaders(
      limit: Limit,
      orderingCondition: OrderingCondition,
      lastSeenHash: Option[Blockhash]
  ): F[List[BlockHeader]]

  def getHeader(blockhash: Blockhash): F[BlockHeader]
}

trait BlockBlockingDataHandler extends BlockDataHandler[ApplicationResult]
