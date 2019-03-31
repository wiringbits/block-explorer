package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.persisted.Block

import scala.language.higherKinds

/**
 * The ledger could be handled as a stack, we should be able to keep the consistency
 * even in the case of reorganizations using just stack operations.
 */
trait LedgerDataHandler[F[_]] {

  /**
   * Append a block to the ledger, the method will succeed only in the following scenarios:
   * - The ledger is empty and the block is the genesis one.
   * - The ledger has some blocks and the block goes just after the latest one.
   */
  def push(block: Block.HasTransactions, tposContracts: List[TPoSContract]): F[Unit]

  /**
   * Remove the latest block from the ledger, it will succeed only if the ledger is not empty.
   */
  def pop(): F[Block]
}

trait LedgerBlockingDataHandler extends LedgerDataHandler[ApplicationResult]
