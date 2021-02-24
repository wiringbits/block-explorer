package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.{
  FutureApplicationResult,
  FuturePaginatedResult
}
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination
import com.alexitc.playsonify.models.pagination.PaginatedQuery
import com.xsn.explorer.data.{BlockBlockingDataHandler, BlockDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.persisted.{Block, BlockHeader, BlockInfo}
import com.xsn.explorer.models.values.{Blockhash, Height}
import javax.inject.Inject

import scala.concurrent.Future

class BlockFutureDataHandler @Inject() (
    blockBlockingDataHandler: BlockBlockingDataHandler,
    retryableFutureDataHandler: RetryableDataHandler
)(implicit
    ec: DatabaseExecutionContext
) extends BlockDataHandler[FutureApplicationResult] {

  override def getBy(blockhash: Blockhash): FutureApplicationResult[Block] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getBy(blockhash)
      }
    }

  override def getBy(height: Height): FutureApplicationResult[Block] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getBy(height)
      }
    }

  override def getBy(
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[BlockField]
  ): FuturePaginatedResult[Block] = retryableFutureDataHandler.retrying {
    Future {
      blockBlockingDataHandler.getBy(paginatedQuery, ordering)
    }
  }

  override def delete(blockhash: Blockhash): FutureApplicationResult[Block] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.delete(blockhash)
      }
    }

  override def getLatestBlock(): FutureApplicationResult[Block] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getLatestBlock()
      }
    }

  override def getFirstBlock(): FutureApplicationResult[Block] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getFirstBlock()
      }
    }

  override def getHeaders(
      limit: pagination.Limit,
      orderingCondition: OrderingCondition,
      lastSeenHash: Option[Blockhash]
  ): FutureApplicationResult[List[BlockHeader]] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getHeaders(
          limit,
          orderingCondition,
          lastSeenHash
        )
      }
    }

  override def getHeader(
      blockhash: Blockhash,
      includeFilter: Boolean
  ): FutureApplicationResult[BlockHeader] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getHeader(blockhash, includeFilter)
      }
    }

  override def getHeader(
      height: Height,
      includeFilter: Boolean
  ): FutureApplicationResult[BlockHeader] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getHeader(height, includeFilter)
      }
    }

  override def getBlocks(
      limit: pagination.Limit,
      orderingCondition: OrderingCondition,
      lastSeenHash: Option[Blockhash]
  ): FutureApplicationResult[List[BlockInfo]] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getBlocks(
          limit,
          orderingCondition,
          lastSeenHash
        )
      }
    }

  override def getBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[BlockInfo] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getBlock(blockhash)
      }
    }

  override def getBlock(height: Height): FutureApplicationResult[BlockInfo] =
    retryableFutureDataHandler.retrying {
      Future {
        blockBlockingDataHandler.getBlock(height)
      }
    }
}
