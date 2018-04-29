package controllers

import javax.inject.Inject

import com.alexitc.playsonify.models.{Limit, Offset, OrderingQuery, PaginatedQuery}
import com.xsn.explorer.services.MasternodeService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class MasternodesController @Inject() (
    masternodeService: MasternodeService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def get(offset: Int, limit: Int, ordering: String) = publicNoInput { _ =>
    val paginatedQuery = PaginatedQuery(Offset(offset), Limit(limit))
    val orderingQuery = OrderingQuery(ordering)

    masternodeService.getMasternodes(paginatedQuery, orderingQuery)
  }
}
