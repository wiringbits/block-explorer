package com.xsn.explorer.services

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits._
import com.xsn.explorer.data.async.{BalanceFutureDataHandler, StatisticsFutureDataHandler}
import com.xsn.explorer.models.{
  AddressesReward,
  MarketStatistics,
  NodeStatistics,
  ROI,
  StatisticsDetails,
  SynchronizationProgress
}
import com.xsn.explorer.services.synchronizer.repository.{MasternodeRepository, MerchantnodeRepository}
import com.xsn.explorer.tasks.CurrencySynchronizerActor
import org.scalactic.{Bad, Good}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.math.BigDecimal.RoundingMode

class StatisticsService @Inject() (
    xsnService: XSNService,
    actorSystem: ActorSystem,
    statisticsFutureDataHandler: StatisticsFutureDataHandler,
    balanceFutureDataHandler: BalanceFutureDataHandler,
    merchantnodeRepository: MerchantnodeRepository,
    masternodeRepository: MasternodeRepository
)(implicit
    ec: ExecutionContext
) {

  def getStatistics(): FutureApplicationResult[StatisticsDetails] = {
    val dbStats = statisticsFutureDataHandler.getStatistics()
    val mnStats = masternodeRepository.getCount()
    val masternodesEnabled = masternodeRepository.getEnabledCount()
    val tposStats = merchantnodeRepository.getCount()
    val difficultyF = xsnService.getDifficulty()

    val result = for {
      stats <- dbStats.toFutureOr
      mnCount <- mnStats.map(x => Good(Some(x))).toFutureOr
      mnEnabledCount <- masternodesEnabled.map(x => Good(Some(x))).toFutureOr
      difficulty <- discardErrors(difficultyF).toFutureOr
      tposCount <- tposStats.map(x => Good(Some(x))).toFutureOr
    } yield StatisticsDetails(
      stats,
      mnCount,
      tposCount,
      difficulty,
      mnEnabledCount
    )

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
    } yield {
      val stats = NodeStatistics(
        masternodes = mnCount,
        enabledMasternodes = mnEnabledCount,
        masternodesProtocols = mnProtocols,
        tposnodes = tposCount,
        enabledTposnodes = tposEnabledCount,
        tposnodesProtocols = tposProtocols
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

  def getSynchronizationProgress: FutureApplicationResult[SynchronizationProgress] = {
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

  def getPrices: FutureApplicationResult[MarketStatistics] = {
    val currencyActor = actorSystem.actorSelection("user/currency_synchronizer")
    implicit val timeout: Timeout = 10.seconds

    currencyActor
      .ask(CurrencySynchronizerActor.GetMarketStatistics)
      .mapTo[MarketStatistics]
      .map(Good(_))
  }

  def getRewardedAddresses(
      period: FiniteDuration
  ): FutureApplicationResult[AddressesReward] = {
    val startDate = Instant.now.minusSeconds(period.toSeconds)

    statisticsFutureDataHandler.getRewardedAddresses(startDate)
  }

  def getROI(
      rewardedAddressesSumLast72Hours: BigDecimal
  ): FutureApplicationResult[ROI] = {
    masternodeRepository.getEnabledCount().map { enabledMasternodes =>
      val mnROI: BigDecimal =
        if (enabledMasternodes > 0)
          (BigDecimal(12960) / (enabledMasternodes * BigDecimal(
            15000
          )) * BigDecimal(365)).setScale(8, RoundingMode.UP)
        else BigDecimal(0)
      val stakingROI: BigDecimal =
        if (rewardedAddressesSumLast72Hours > 0)
          ((BigDecimal(12960) / rewardedAddressesSumLast72Hours) * BigDecimal(
            365
          )).setScale(8, RoundingMode.UP)
        else BigDecimal(0)
      Good(ROI(masternodes = mnROI, staking = stakingROI))
    }
  }

  def getStakingCoins(): FutureApplicationResult[BigDecimal] = {
    statisticsFutureDataHandler.getStakingCoins()
  }

  private def discardErrors[T](
      value: FutureApplicationResult[T]
  ): FutureApplicationResult[Option[T]] = {
    value
      .map {
        case Good(result) => Good(Some(result))
        case Bad(_) => Good(None)
      }
      .recover { case _: Throwable => Good(None) }
  }
}
