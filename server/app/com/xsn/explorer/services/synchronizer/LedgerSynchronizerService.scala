package com.xsn.explorer.services.synchronizer

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.core.{FutureApplicationResult, FutureOr}
import com.xsn.explorer.config.LedgerSynchronizerConfig
import com.xsn.explorer.data.async.LedgerFutureDataHandler
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models._
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.XSNService
import com.xsn.explorer.services.synchronizer.repository.BlockChunkRepository
import javax.inject.Inject
import org.scalactic.{Bad, Good, One}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LedgerSynchronizerService @Inject()(
    synchronizerConfig: LedgerSynchronizerConfig,
    xsnService: XSNService,
    ledgerDataHandler: LedgerFutureDataHandler,
    syncStatusService: LedgerSynchronizationStatusService,
    syncOps: LedgerSynchronizationOps,
    blockChunkRepository: BlockChunkRepository.FutureImpl,
    blockParallelChunkSynchronizer: BlockParallelChunkSynchronizer
)(implicit ec: ExecutionContext)
    extends LedgerSynchronizer {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Synchronize the given block with our ledger database.
   *
   * This method must not be called concurrently, otherwise, it is likely that
   * the database will get corrupted.
   */
  def synchronize(blockhash: Blockhash): FutureApplicationResult[Unit] = {
    val result = for {
      _ <- applyPendingUpdate().toFutureOr
      candidate <- syncOps.getRPCBlock(blockhash).toFutureOr
      status <- syncStatusService.getSyncingStatus(candidate).toFutureOr
      _ <- sync(status).toFutureOr
    } yield ()

    result.toFuture
  }

  private def applyPendingUpdate(): FutureApplicationResult[Unit] = {
    val result = for {
      blockhashMaybe <- blockChunkRepository.findSyncingBlock().toFutureOr
    } yield blockhashMaybe match {
      case None => Future.successful(Good(())).toFutureOr
      case Some(blockhash) =>
        logger.info(s"The block $blockhash was being synced, retrying")
        val inner = for {
          block <- syncOps.getFullRPCBlock(blockhash).toFutureOr
          data <- syncOps.getBlockData(block).toFutureOr
          _ <- blockParallelChunkSynchronizer.sync(data._1, data._2, data._3, data._4).toFutureOr
        } yield ()

        inner.toFuture.flatMap {
          case Good(_) => Future.successful(Good(()))
          case Bad(One(BlockNotFoundError)) =>
            logger.warn(s"The block $blockhash was partially synced but is not in the node anymore, rolling back")
            blockParallelChunkSynchronizer.rollback(blockhash)
          case Bad(e) =>
            logger.warn(s"The block $blockhash was partially synced but it wasn't retrieved from the node, errors = $e")
            Future.successful(Bad(e))
        }.toFutureOr
    }

    result.flatMap(identity).toFuture
  }

  private def sync(status: SynchronizationStatus): FutureApplicationResult[Unit] = {
    status match {
      case SynchronizationStatus.Synced =>
        Future.successful(Good(()))

      case SynchronizationStatus.MissingBlockInterval(goal) =>
        if (goal.length >= 10) {
          logger.info(s"Syncing block interval $goal")
        }

        val result = for {
          _ <- sync(goal).toFutureOr
        } yield {
          if (goal.length >= 10) {
            logger.info("Synchronization completed")
          }
        }
        result.toFuture

      case SynchronizationStatus.PendingReorganization(cutPoint, goal) =>
        logger.info(s"Applying reorganization, cutPoint = $cutPoint, goal = $goal")
        val result = for {
          latestRemoved <- rollback(cutPoint)
          interval = latestRemoved.height.int to goal.int
          _ = logger.info(s"${latestRemoved.hash} rolled back, now syncing $interval")
          _ <- sync(latestRemoved.height.int to goal.int).toFutureOr
        } yield {
          logger.info("Reorganization completed")
        }

        result.toFuture
    }
  }

  private def sync(goal: Range): FutureApplicationResult[Unit] = {
    goal.foldLeft[FutureApplicationResult[Unit]](Future.successful(Good(()))) {
      case (previous, height) =>
        val result = for {
          _ <- previous.toFutureOr
          blockhash <- xsnService.getBlockhash(Height(height)).toFutureOr
          block <- syncOps.getFullRPCBlock(blockhash).toFutureOr
          _ <- append(block).toFutureOr
        } yield ()

        result.toFuture
    }
  }

  private def rollback(goal: BlockPointer): FutureOr[persisted.Block] = {
    ledgerDataHandler
      .pop()
      .toFutureOr
      .flatMap { removedBlock =>
        if (removedBlock.previousBlockhash.contains(goal.blockhash)) {
          Future.successful(Good(removedBlock)).toFutureOr
        } else {
          require(
            goal.height.int <= removedBlock.height.int,
            "Can't rollback more, we have reached the supposed cut point without finding its hash"
          )
          rollback(goal)
        }
      }
  }

  private def append(newBlock: rpc.Block.HasTransactions[_]): FutureApplicationResult[Unit] = {
    val result = for {
      data <- syncOps.getBlockData(newBlock).toFutureOr
      (blockWithTransactions, tposContracts, filterFactory, rewards) = data
      _ <- if (synchronizerConfig.parallelSynchronizer) {
        blockParallelChunkSynchronizer
          .sync(blockWithTransactions.asTip, tposContracts, filterFactory, rewards)
          .toFutureOr
      } else {
        ledgerDataHandler.push(blockWithTransactions.asTip, tposContracts, filterFactory, rewards).toFutureOr
      }
    } yield {
      if (blockWithTransactions.height.int % 5000 == 0) {
        logger.info(s"Caught up to block ${blockWithTransactions.height}")
      }
    }

    result.toFuture
  }
}
