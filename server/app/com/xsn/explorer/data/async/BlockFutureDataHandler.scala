package com.xsn.explorer.data.async

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{BlockBlockingDataHandler, BlockDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block

import scala.concurrent.Future

class BlockFutureDataHandler @Inject() (
    blockBlockingDataHandler: BlockBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext)
    extends BlockDataHandler[FutureApplicationResult] {

  def upsert(block: Block): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.upsert(block)
  }

  def getBy(blockhash: Blockhash): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.getBy(blockhash)
  }

  def delete(blockhash: Blockhash): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.delete(blockhash)
  }

  def getLatestBlock(): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.getLatestBlock()
  }

  def getFirstBlock(): FutureApplicationResult[Block] = Future {
    blockBlockingDataHandler.getFirstBlock()
  }
}
