package com.xsn.explorer.loggers

import org.slf4j.Logger
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models._

case class LedgerSynchronizerServiceLogger(
    logger: Logger,
    synchedBlocksCount: Int,
    reorganization: Boolean,
    reorganizationCuttingPoint: Option[Block],
    reorganizationCuttingPointReached: Boolean
) {

  def syncCompleted(lastBlock: rpc.Block): LedgerSynchronizerServiceLogger = {
    if (synchedBlocksCount == 0) {
      logger.info(s"Ledger up to date at height ${lastBlock.height.int}")
    } else {
      if (reorganization) {
        logger.info(
          reorganizationCuttingPoint
            .map(
              cuttingPoint => s"Blockchain reorganized starting at ${cuttingPoint.hash}, finished at ${lastBlock.hash}"
            )
            .getOrElse("reorganization without common block")
        )
      } else {
        logger.info(s"Sync completed at height ${lastBlock.height.int}, $synchedBlocksCount blocks synched")
      }
    }

    this
  }

  def blockSynched(previousBlock: Option[Block], block: rpc.Block): LedgerSynchronizerServiceLogger = {
    if (block.height.int % 5000 == 0) {
      logger.info(s"Synced block ${block.hash} at height ${block.height.int}")
    }

    if (reorganization && !reorganizationCuttingPointReached) {
      return this.copy(
        reorganizationCuttingPoint = previousBlock,
        reorganizationCuttingPointReached = true,
        synchedBlocksCount = synchedBlocksCount + 1
      )
    }
    this.copy(synchedBlocksCount = synchedBlocksCount + 1)
  }

  def reorganize: LedgerSynchronizerServiceLogger = {
    this.copy(reorganization = true)
  }

  def trimed: LedgerSynchronizerServiceLogger = {
    this.copy(reorganization = true)
  }
}

object LedgerSynchronizerServiceLogger {

  def apply(logger: Logger): LedgerSynchronizerServiceLogger = {
    new LedgerSynchronizerServiceLogger(logger, 0, false, None, false)
  }
}
