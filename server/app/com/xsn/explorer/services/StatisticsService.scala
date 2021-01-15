package com.xsn.explorer.services

import akka.pattern.ask
import akka.actor.ActorSystem
import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits._
import com.xsn.explorer.data.async.{
  StatisticsFutureDataHandler,
  BalanceFutureDataHandler
}
import com.xsn.explorer.models.{
  MarketStatistics,
  StatisticsDetails,
  NodeStatistics,
  SynchronizationProgress
}
import com.xsn.explorer.tasks.CurrencySynchronizerActor
import com.xsn.explorer.services.synchronizer.repository.{
  MasternodeRepository,
  MerchantnodeRepository,
  NodeStatsRepository
}
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class StatisticsService @Inject() (
    xsnService: XSNService,
    actorSystem: ActorSystem,
    statisticsFutureDataHandler: StatisticsFutureDataHandler,
    balanceFutureDataHandler: BalanceFutureDataHandler,
    merchantnodeRepository: MerchantnodeRepository,
    masternodeRepository: MasternodeRepository,
    nodeStatsRepository: NodeStatsRepository
)(implicit
    ec: ExecutionContext
) {

  def getStatistics(): FutureApplicationResult[StatisticsDetails] = {
    val dbStats = statisticsFutureDataHandler.getStatistics()
    val mnStats = masternodeRepository.getCount()
    val tposStats = merchantnodeRepository.getCount()
    val difficultyF = xsnService.getDifficulty()

    val result = for {
      stats <- dbStats.toFutureOr
      mnCount <- mnStats.map(x => Good(Some(x))).toFutureOr
      difficulty <- discardErrors(difficultyF).toFutureOr
      tposCount <- tposStats.map(x => Good(Some(x))).toFutureOr
    } yield StatisticsDetails(stats, mnCount, tposCount, difficulty)

    result.toFuture
  }

  def getNodeStatistics(): FutureApplicationResult[NodeStatistics] = {
    for {
      mnCount <- masternodeRepository.getCount()
      mnEnabledCount <- masternodeRepository.getEnabledCount()
      mnProtocols <- masternodeRepository.getProtocols()
      tposCount <- merchantnodeRepository.getCount()
      tposEnabledCount <- merchantnodeRepository.getEnabledCount()
      tposProtocols <- merchantnodeRepository.getProtocols()
      coinsStaking <- nodeStatsRepository.getCoinsStaking()
    } yield {
      val stats = NodeStatistics(
        masternodes = mnCount,
        enabledMasternodes = mnEnabledCount,
        masternodesProtocols = mnProtocols,
        tposnodes = tposCount,
        enabledTposnodes = tposEnabledCount,
        tposnodesProtocols = tposProtocols,
        coinsStaking = coinsStaking
      )
      Good(stats)
    }
  }

  def getCoinsStaking(): FutureApplicationResult[BigDecimal] = {
    val tposNodes = merchantnodeRepository.getAll()

    val result = for {
      tposNodesList <- tposNodes.map(x => Good(x)).toFutureOr

      tposAddressList <- tposNodesList
        .map(t => t.payee)
        .map(statisticsFutureDataHandler.getTPoSMerchantStakingAddresses)
        .toFutureOr
      coinsStaking <- tposAddressList.flatten
        .map(balanceFutureDataHandler.getBy)
        .toFutureOr
      coinsStakingSum = coinsStaking.map(t => t.available).sum
    } yield coinsStakingSum

    result.toFuture
  }

  def getSynchronizationProgress
      : FutureApplicationResult[SynchronizationProgress] = {
    val dbStats = statisticsFutureDataHandler.getStatistics()
    val rpcBlock = xsnService.getLatestBlock()

    val result = for {
      stats <- dbStats.toFutureOr
      latestBlock <- rpcBlock.toFutureOr
    } yield SynchronizationProgress(
      total = latestBlock.height.int,
      synced = stats.blocks
    )

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

  private def discardErrors[T](
      value: FutureApplicationResult[T]
  ): FutureApplicationResult[Option[T]] = {
    value
      .map {
        case Good(result) => Good(Some(result))
        case Bad(_)       => Good(None)
      }
      .recover { case _: Throwable => Good(None) }
  }
}
