package controllers

import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.services.BalanceService
import controllers.common.{Codecs, MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject

class BalancesController @Inject()(balanceService: BalanceService, cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Codecs._

  def getHighest(limit: Int, lastSeenAddress: Option[String]) = public { _ =>
    balanceService.getHighest(Limit(limit), lastSeenAddress)
  }
}
