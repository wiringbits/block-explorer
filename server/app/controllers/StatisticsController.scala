package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.services.StatisticsService
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.duration._

class StatisticsController @Inject() (
    statisticsService: StatisticsService,
    cc: MyJsonControllerComponents
) extends MyJsonController(cc) {

  def getStatus() = public { _ =>
    statisticsService
      .getStatistics()
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getNodeStatus() = public { _ =>
    statisticsService
      .getNodeStatistics()
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getBlockRewardsSummary() = public { _ =>
    val response = for {
      blockRewards <- statisticsService.getRewardsSummary(1000).toFutureOr
      rewardedAddresses <- statisticsService.getRewardedAddresses(72.hours).toFutureOr

      rewardsJson = Json.toJson(blockRewards).as[JsObject]
      result = rewardsJson + ("rewardedAddressesCountLast72Hours" -> Json.toJson(rewardedAddresses.addressesNumber)) +
        ("rewardedAddressesSumLast72Hours" -> Json.toJson(rewardedAddresses.amount))
    } yield {
      Ok(result).withHeaders("Cache-Control" -> "public, max-age=3600")
    }

    response.toFuture
  }

  def getCurrency(currency: Option[String]) = public { _ =>
    val prices = statisticsService.getPrices.toFutureOr.map(r => Json.toJson(r))

    val result = currency.map(_.toLowerCase) match {
      case Some(currency) =>
        prices.map { prices =>
          (prices \ currency)
            .asOpt[BigDecimal]
            .map(price => Ok(Json.obj(currency -> price)))
            .getOrElse(NotFound)
        }
      case None => prices.map(Ok(_))
    }

    result.future
  }
}
