package com.xsn.explorer.data.async

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models.{FieldOrdering, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.{BlockBlockingDataHandler, BlockDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Height}

import scala.concurrent.Future

class BlockFutureDataHandler @Inject() (
    blockBlockingDataHandler: BlockBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext)
    extends BlockDataHandler[FutureApplicationResult] {

  override def getBy(blockhash: Blockhash): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.getBy(blockhash)
  }

  override def getBy(height: Height): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.getBy(height)
  }

  override def getBy(paginatedQuery: PaginatedQuery, ordering: FieldOrdering[BlockField]): FutureApplicationResult[PaginatedResult[Block]] = Future {
    blockBlockingDataHandler.getBy(paginatedQuery, ordering)
  }

  override def delete(blockhash: Blockhash): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.delete(blockhash)
  }

  override def getLatestBlock(): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.getLatestBlock()
  }

  override def getFirstBlock(): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.getFirstBlock()
  }
}
