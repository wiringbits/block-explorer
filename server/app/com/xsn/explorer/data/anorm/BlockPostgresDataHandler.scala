package com.xsn.explorer.data.anorm

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination
import com.alexitc.playsonify.models.pagination.{PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.BlockBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.BlockPostgresDAO
import com.xsn.explorer.errors._
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.persisted.{Block, BlockHeader}
import com.xsn.explorer.models.values.{Blockhash, Height}
import javax.inject.Inject
import org.scalactic.{Good, One, Or}
import play.api.db.Database

class BlockPostgresDataHandler @Inject()(override val database: Database, blockPostgresDAO: BlockPostgresDAO)
    extends BlockBlockingDataHandler
    with AnormPostgresDataHandler {

  override def getBy(blockhash: Blockhash): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.getBy(blockhash)
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def getBy(height: Height): ApplicationResult[Block] = withConnection { implicit conn =>
    val maybe = blockPostgresDAO.getBy(height)
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def getBy(
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[BlockField]
  ): ApplicationResult[PaginatedResult[Block]] = withConnection { implicit conn =>
    val data = blockPostgresDAO.getBy(paginatedQuery, ordering)
    val total = blockPostgresDAO.count
    val result = PaginatedResult(paginatedQuery.offset, paginatedQuery.limit, total, data)

    Good(result)
  }

  override def delete(blockhash: Blockhash): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.delete(blockhash)
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def getLatestBlock(): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.getLatestBlock
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def getFirstBlock(): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.getFirstBlock
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def getHeaders(
      limit: pagination.Limit,
      orderingCondition: OrderingCondition,
      lastSeenHash: Option[Blockhash]
  ): ApplicationResult[List[BlockHeader]] = withConnection { implicit conn =>
    val result = lastSeenHash
      .map { hash =>
        blockPostgresDAO.getHeaders(hash, limit, orderingCondition)
      }
      .getOrElse { blockPostgresDAO.getHeaders(limit, orderingCondition) }

    Good(result)
  }
}
