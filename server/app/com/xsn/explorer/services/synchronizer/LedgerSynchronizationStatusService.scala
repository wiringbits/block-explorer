package com.xsn.explorer.services.synchronizer

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.core.{FutureApplicationResult, FutureOr}
import com.xsn.explorer.data.async.BlockFutureDataHandler
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models.{BlockPointer, persisted, rpc}
import com.xsn.explorer.services.XSNService
import com.xsn.explorer.util.Extensions.FutureOrExt
import javax.inject.Inject
import org.scalactic.{Bad, Good}

import scala.concurrent.{ExecutionContext, Future}

private[synchronizer] class LedgerSynchronizationStatusService @Inject() (
    syncOps: LedgerSynchronizationOps,
    xsnService: XSNService,
    blockDataHandler: BlockFutureDataHandler
)(implicit ec: ExecutionContext) {

  /** Lets define some values:
    * - The candidate is the block that needs to be stored.
    * - The latestLedgerBlock is the newer block that is stored.
    * - The LCA is the least common ancestor between both chains.
    *
    * There are some trivial cases to handle:
    * - There are no blocks stored, just sync everything.
    * - The candidate is already stored, just ignore it.
    *
    * So, let's assume that:
    * - The candidate is not stored.
    * - We have at least a block stored (latestLedgerBlock).
    * - The LCA is on the candidate's chain, and on our stored blocks.
    *
    * Then, we can apply the candidate by rolling back until the LCA, then, applying missing blocks until catching up
    * the candidate.
    *
    * @param candidate the block that we need to store
    * @return the state that needs to be applied in order to store the candidate block
    */
  def getSyncingStatus(
      candidate: rpc.Block[_]
  ): FutureApplicationResult[SynchronizationStatus] = {
    syncOps.getLatestLedgerBlock.toFutureOr.flatMap {
      case Some(latestLedgerBlock) =>
        for {
          lca <- findLeastCommonAncestor(candidate, latestLedgerBlock)
        } yield {
          if (lca.blockhash == candidate.hash) {
            // nothing to do
            SynchronizationStatus.Synced
          } else if (lca.blockhash == latestLedgerBlock.hash) {
            // apply missing blocks
            SynchronizationStatus.MissingBlockInterval(
              latestLedgerBlock.height.int to candidate.height.int
            )
          } else {
            // rollback and apply missing blocks
            SynchronizationStatus.PendingReorganization(lca, candidate.height)
          }
        }

      case None =>
        val result =
          SynchronizationStatus.MissingBlockInterval(0 to candidate.height.int)
        Future.successful(Good(result)).toFutureOr

    }.toFuture
  }

  def findLeastCommonAncestor(
      candidate: rpc.Block[_],
      existing: persisted.Block
  ): FutureOr[BlockPointer] = {
    if (candidate.height.int <= existing.height.int) {
      // the candidate might be already stored
      // otherwise, find the newest block from the chain that is stored in the database
      findNewestStoredBlockFromChain(candidate)
    } else {
      // the candidate is not stored
      // find the newest stored block that is in the chain
      findNewestChainBlockFromStorage(existing)
    }
  }

  private def findNewestStoredBlockFromChain(
      candidate: rpc.Block[_]
  ): FutureOr[BlockPointer] = {
    blockDataHandler
      .getBy(candidate.hash)
      .toFutureOr
      .map(Option.apply)
      .recoverFrom(BlockNotFoundError)(None)
      .flatMap {
        case Some(_) =>
          Future
            .successful(Good(BlockPointer(candidate.hash, candidate.height)))
            .toFutureOr

        case None =>
          candidate.previousBlockhash match {
            case Some(previous) =>
              xsnService
                .getBlock(previous)
                .toFutureOr
                .flatMap(findNewestStoredBlockFromChain)
            case None =>
              Future.successful(Bad(BlockNotFoundError).accumulating).toFutureOr
          }
      }
  }

  private def findNewestChainBlockFromStorage(
      block: persisted.Block
  ): FutureOr[BlockPointer] = {
    xsnService
      .getBlockhash(block.height)
      .toFutureOr
      .map(Option.apply)
      .recoverFrom(BlockNotFoundError)(None)
      .flatMap {
        case Some(blockhash) if blockhash == block.hash =>
          Future
            .successful(Good(BlockPointer(block.hash, block.height)))
            .toFutureOr

        case _ =>
          block.previousBlockhash match {
            case Some(previous) =>
              blockDataHandler
                .getBy(previous)
                .toFutureOr
                .flatMap(findNewestChainBlockFromStorage)
            case None =>
              Future.successful(Bad(BlockNotFoundError).accumulating).toFutureOr
          }
      }
  }
}
