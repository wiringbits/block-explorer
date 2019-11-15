package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.async.StatisticsFutureDataHandler
import com.xsn.explorer.models.{StatisticsDetails, SynchronizationProgress}
import javax.inject.Inject
import org.scalactic.{Bad, Good}

import scala.concurrent.ExecutionContext

class StatisticsService @Inject()(xsnService: XSNService, statisticsFutureDataHandler: StatisticsFutureDataHandler)(
    implicit ec: ExecutionContext
) {

  def getStatistics(): FutureApplicationResult[StatisticsDetails] = {
    val dbStats = statisticsFutureDataHandler.getStatistics()
    val rpcStats = xsnService.getMasternodeCount()
    val difficultyF = xsnService.getDifficulty()

    val result = for {
      stats <- dbStats.toFutureOr
      count <- discardErrors(rpcStats).toFutureOr
      difficulty <- discardErrors(difficultyF).toFutureOr
    } yield StatisticsDetails(stats, count, difficulty)

    result.toFuture
  }

  def getSynchronizationProgress: FutureApplicationResult[SynchronizationProgress] = {
    val dbStats = statisticsFutureDataHandler.getStatistics()
    val rpcBlock = xsnService.getLatestBlock()

    val result = for {
      stats <- dbStats.toFutureOr
      latestBlock <- rpcBlock.toFutureOr
    } yield SynchronizationProgress(total = latestBlock.height.int, synced = stats.blocks)

    result.toFuture
  }

  def getRewardsSummary(numberOfBlocks: Int) = {
    statisticsFutureDataHandler.getRewardsSummary(numberOfBlocks)
  }

  private def discardErrors[T](value: FutureApplicationResult[T]): FutureApplicationResult[Option[T]] = {
    value
      .map {
        case Good(result) => Good(Some(result))
        case Bad(_) => Good(None)
      }
      .recover { case _: Throwable => Good(None) }
  }
}
