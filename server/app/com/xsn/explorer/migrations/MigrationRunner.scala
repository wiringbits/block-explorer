package com.xsn.explorer.migrations

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.anorm.AnormPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.BlockRewardPostgresDAO
import com.xsn.explorer.data.async.BlockFutureDataHandler
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.{BlockExtractionMethod, BlockRewards}
import com.xsn.explorer.models.values.{Blockhash, Height}
import com.xsn.explorer.services.{BlockService, XSNService}
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory
import play.api.db.Database

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MigrationRunner @Inject()(
    xsnService: XSNService,
    blockService: BlockService,
    blockDataHandler: BlockFutureDataHandler,
    db: MigrationRunner.DatabaseOperations
)(
    implicit ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def run() = {
    val targetBlock = blockDataHandler.getLatestBlock().toFutureOr

    targetBlock.map { targetBlock =>
      logger.info(s"Migrating Block Rewards staked output from block 0 to block ${targetBlock.height}")

      val startingState = Future.successful(Good(())).toFutureOr
      val finalState = (0 to targetBlock.height.int).foldLeft(startingState) {
        case (state, height) =>
          for {
            _ <- state
            block <- blockDataHandler.getBy(Height(height)).toFutureOr
            _ <- block.extractionMethod match {
              case BlockExtractionMethod.ProofOfStake | BlockExtractionMethod.TrustlessProofOfStake =>
                updateRewardStakedInfo(block)
              case _ =>
                Future.successful(Good(())).toFutureOr
            }
            _ = logProgress(height, targetBlock.height.int)
          } yield ()
      }

      finalState.toFuture.onComplete {
        case Success(Good(_)) =>
          logger.info("Block rewards staked info successfully migrated")
        case Success(Bad(error)) =>
          logger.info(s"Block rewards staked info migration failed due to ${error.toString}")
        case Failure(error) =>
          logger.info(s"Block rewards staked info migration failed due to ${error.toString}")
      }
    }
  }

  private def updateRewardStakedInfo(block: Block) = {
    for {
      rpcBlock <- xsnService.getBlock(block.hash).toFutureOr
      result <- blockService
        .getBlockRewards(rpcBlock, block.extractionMethod)
        .flatMap {
          case Good(reward) => db.upsertReward(block.hash, reward)
          case Bad(_) => Future.successful(Good(()))
        }
        .toFutureOr
    } yield result
  }

  private def logProgress(migratedHeight: Int, targetHeight: Int) = {
    val percentage: Int = 100 * migratedHeight / targetHeight
    val previousPercentage: Int = 100 * (migratedHeight - 1) / targetHeight

    if (percentage != 0 && percentage != previousPercentage) {
      logger.info(s"migrated block at height ${migratedHeight}, ${percentage}% done")
    }
  }
}

object MigrationRunner {

  class DatabaseOperations @Inject()(override val database: Database, blockRewardPostgresDAO: BlockRewardPostgresDAO)(
      implicit dbEC: DatabaseExecutionContext
  ) extends AnormPostgresDataHandler {

    def upsertReward(blockhash: Blockhash, reward: BlockRewards) = {
      Future {
        withConnection { implicit conn =>
          Good(blockRewardPostgresDAO.upsert(blockhash, reward))
        }
      }
    }
  }
}
