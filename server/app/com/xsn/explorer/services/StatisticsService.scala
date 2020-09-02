package com.xsn.explorer.services

import akka.pattern.ask
import akka.actor.ActorSystem
import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.data.async.StatisticsFutureDataHandler
import com.xsn.explorer.models.{MarketStatistics, StatisticsDetails, SynchronizationProgress}
import com.xsn.explorer.tasks.CurrencySynchronizerActor
import com.xsn.explorer.services.synchronizer.repository.MerchantnodeRepository
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class StatisticsService @Inject()(
    xsnService: XSNService,
    actorSystem: ActorSystem,
    statisticsFutureDataHandler: StatisticsFutureDataHandler,
    merchantnodeRepository: MerchantnodeRepository
)(
    implicit ec: ExecutionContext
) {

  def getStatistics(): FutureApplicationResult[StatisticsDetails] = {
    val dbStats = statisticsFutureDataHandler.getStatistics()
    val mnStats = xsnService.getMasternodeCount()
    val tposStats = merchantnodeRepository.getCount()
    val difficultyF = xsnService.getDifficulty()

    val result = for {
      stats <- dbStats.toFutureOr
      mnCount <- discardErrors(mnStats).toFutureOr
      difficulty <- discardErrors(difficultyF).toFutureOr
      tposCount <- tposStats.map(x => Good(Some(x))).toFutureOr
    } yield StatisticsDetails(stats, mnCount, tposCount, difficulty)

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

  def getPrices(): FutureApplicationResult[MarketStatistics] = {
    val currencyActor = actorSystem.actorSelection("user/currency_synchronizer")
    implicit val timeout: Timeout = 10.seconds

    currencyActor
      .ask(CurrencySynchronizerActor.GetMarketStatistics)
      .mapTo[MarketStatistics]
      .map(Good(_))
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
