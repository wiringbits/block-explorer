package controllers

import javax.inject.Inject

import com.xsn.explorer.models.base.{Limit, Offset, OrderingQuery, PaginatedQuery}
import com.xsn.explorer.services.BalanceService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class BalancesController @Inject() (
    balanceService: BalanceService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def get(offset: Int, limit: Int, ordering: String) = publicNoInput { _ =>
    val paginatedQuery = PaginatedQuery(Offset(offset), Limit(limit))
    val orderingQuery = OrderingQuery(ordering)

    balanceService.get(paginatedQuery, orderingQuery)
  }
}