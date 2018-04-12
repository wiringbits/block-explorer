package controllers

import javax.inject.Inject

import com.xsn.explorer.models.base.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.services.BalanceService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class BalancesController @Inject() (
    balanceService: BalanceService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getRichest(offset: Int, limit: Int) = publicNoInput { _ =>
    val query = PaginatedQuery(Offset(offset), Limit(limit))
    balanceService.getRichest(query)
  }
}