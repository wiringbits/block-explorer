package com.xsn.explorer.loggers

import org.slf4j.Logger
import com.xsn.explorer.models._

class LedgerSynchronizerServiceLogger(logger: Logger, synchedBlocks: List[rpc.Block]) {
  def syncCompleted(lastBlock: rpc.Block) = {
    if(synchedBlocks.isEmpty) {
      logger.info(s"Ledger up to date at height ${lastBlock.height.int}")
    }else{
      logger.info(s"Sync completed, ${synchedBlocks.size} blocks synched")
    }
  }
  def blockSynched(block: rpc.Block): LedgerSynchronizerServiceLogger = {
    if(block.height.int % 5000 == 0) {
      logger.info(s"Synced block ${block.hash} at height ${block.height.int}")
    }
    LedgerSynchronizerServiceLogger(logger, block :: synchedBlocks)
  }
}

object LedgerSynchronizerServiceLogger {
  def apply(logger: Logger): LedgerSynchronizerServiceLogger = {
    new LedgerSynchronizerServiceLogger(logger, List[rpc.Block]())
  }

  def apply(logger: Logger, synchedBlocks: List[rpc.Block]): LedgerSynchronizerServiceLogger = {
    new LedgerSynchronizerServiceLogger(logger, synchedBlocks)
  }
}