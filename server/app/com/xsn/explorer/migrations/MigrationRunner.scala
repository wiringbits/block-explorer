package com.xsn.explorer.migrations

import anorm.SQL
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.anorm.AnormPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.{BlockPostgresDAO, BlockRewardPostgresDAO}
import com.xsn.explorer.data.async.BlockFutureDataHandler
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.{BlockExtractionMethod, BlockRewards, rpc}
import com.xsn.explorer.models.persisted.Block
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
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def run() = {
    val targetBlock = blockDataHandler.getLatestBlock().toFutureOr

    targetBlock.map { targetBlock =>
      logger.info(s"Migrating Block Rewards from block 0 to block ${targetBlock.height}")

      val startingState = Future.successful(Good(())).toFutureOr
      val finalState = (0 to targetBlock.height.int).foldLeft(startingState) {
        case (state, height) =>
          for {
            _ <- state
            block <- blockDataHandler.getBy(Height(height)).toFutureOr
            rpcBlock <- xsnService.getFullBlock(block.hash).toFutureOr
            _ <- block.extractionMethod match {
              case BlockExtractionMethod.TrustlessProofOfStake =>
                recalculateExtractionMethod(block, rpcBlock).toFutureOr
              case _ => Future.successful(Good(())).toFutureOr
            }
            _ <- db.getReward(block.hash).toFutureOr.flatMap {
              case None => storeBlockReward(rpcBlock).toFutureOr
              case _ => Future.successful(Good(())).toFutureOr
            }
            _ = logProgress(height, targetBlock.height.int)
          } yield ()
      }

      finalState.toFuture.onComplete {
        case Success(Good(_)) => logger.info("Block rewards successfully migrated")
        case Success(Bad(error)) => logger.info(s"Block reward migration failed due to ${error.toString}")
        case Failure(error) => logger.info(s"Block reward migration failed due to ${error.toString}")
      }
    }
  }

  private def logProgress(migratedHeight: Int, targetHeight: Int) = {
    val percentage: Int = 100 * migratedHeight / targetHeight
    val previousPercentage: Int = 100 * (migratedHeight - 1) / targetHeight

    if (percentage != 0 && percentage != previousPercentage) {
      logger.info(s"migrated block at height ${migratedHeight}, ${percentage}% done")
    }
  }

  private def recalculateExtractionMethod(block: Block, rpcBlock: rpc.Block[_]) = {
    val result = for {
      extractionMethod <- blockService.extractionMethod(rpcBlock).toFutureOr
      _ <- db.updateExtractionMethod(block.hash, extractionMethod).toFutureOr
    } yield Good(())

    result.future
  }

  private def storeBlockReward(rpcBlock: rpc.Block[_]) = {
    val result = for {
      extractionMethod <- blockService.extractionMethod(rpcBlock).toFutureOr
      reward <- blockService
        .getBlockRewards(rpcBlock, extractionMethod)
        .map {
          case Good(reward) => Good(Some(reward))
          case Bad(_) => Good(None)
        }
        .toFutureOr
      _ = reward.foreach(r => db.insertBlockRewards(rpcBlock.hash, r))
    } yield Good(())

    result.future
  }
}

object MigrationRunner {
  private class DatabaseOperations @Inject()(
      override val database: Database,
      blockPostgresDAO: BlockPostgresDAO,
      blockRewardPostgresDAO: BlockRewardPostgresDAO
  )(
      implicit dbEC: DatabaseExecutionContext
  ) extends AnormPostgresDataHandler {

    def updateExtractionMethod(blockhash: Blockhash, extractionMethod: BlockExtractionMethod) = {
      Future {
        withConnection { implicit conn =>
          SQL(
            """
              |UPDATE blocks
              |SET extraction_method = {extraction_method}::BLOCK_EXTRACTION_METHOD_TYPE
              |WHERE blockhash = {blockhash}
      """.stripMargin
          ).on(
              'blockhash -> blockhash.toBytesBE.toArray,
              'extraction_method -> extractionMethod.entryName
            )
            .execute

          Good(())
        }
      }
    }

    def getReward(blockhash: Blockhash) = {
      Future {
        withConnection { implicit conn =>
          Good(blockRewardPostgresDAO.getBy(blockhash))
        }
      }
    }

    def insertBlockRewards(blockhash: Blockhash, reward: BlockRewards) = {
      Future {
        withConnection { implicit conn =>
          Good(blockRewardPostgresDAO.upsert(blockhash, reward))
        }
      }
    }
  }
}
