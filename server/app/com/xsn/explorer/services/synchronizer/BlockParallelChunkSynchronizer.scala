package com.xsn.explorer.services.synchronizer

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.values.Blockhash
import com.xsn.explorer.models.{BlockRewards, TPoSContract}
import com.xsn.explorer.services.synchronizer.operations.BlockParallelChunkAddOps
import com.xsn.explorer.services.synchronizer.repository.BlockChunkRepository
import javax.inject.Inject
import kamon.Kamon
import org.scalactic.Good
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class BlockParallelChunkSynchronizer @Inject() (
    blockChunkRepository: BlockChunkRepository.FutureImpl,
    addOps: BlockParallelChunkAddOps
)(implicit
    ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Synchronize the given block (continuing from the last step if it is partially synchronized).
    */
  def sync(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val start = System.currentTimeMillis()
    val timer = Kamon
      .timer("syncSingleBlock")
      .withTag("height", block.height.int.toLong)
      .withTag("hash", block.hash.string)
      .start()

    val result = for {
      stateMaybe <- blockChunkRepository.findSyncState(block.hash).toFutureOr
      currentState = stateMaybe.getOrElse(
        BlockSynchronizationState.StoringBlock
      )
      _ <- addOps
        .continueFromState(
          currentState,
          block,
          tposContracts,
          filterFactory,
          rewards
        )
        .toFutureOr
      _ = logger.debug(
        s"Synced ${block.height}, took ${System.currentTimeMillis() - start} ms"
      )
    } yield ()

    result.toFuture.onComplete(_ => timer.stop())

    result.toFuture
  }

  def rollback(blockhash: Blockhash): FutureApplicationResult[Unit] = {
    val result = for {
      stateMaybe <- blockChunkRepository.findSyncState(blockhash).toFutureOr
    } yield stateMaybe match {
      case None =>
        logger.warn(
          s"The block $blockhash is supposed to be rolled back but it is not syncing"
        )
        Future.successful(Good(())).toFutureOr
      case Some(state) =>
        logger.warn(
          s"The block $blockhash is going to be rolled back from the $state state"
        )
        blockChunkRepository.atomicRollback(blockhash).toFutureOr
    }

    result.flatMap(identity).toFuture
  }
}
