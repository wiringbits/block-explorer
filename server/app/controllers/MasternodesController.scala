package controllers

import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.services.MasternodeService
import controllers.common.{Codecs, MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject

class MasternodesController @Inject() (
    masternodeService: MasternodeService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Codecs._

  def get(offset: Int, limit: Int, ordering: String) = public { _ =>
    val paginatedQuery = PaginatedQuery(Offset(offset), Limit(limit))
    val orderingQuery = OrderingQuery(ordering)

    masternodeService.getMasternodes(paginatedQuery, orderingQuery)
  }

  def getBy(ipAddress: String) = public { _ =>
    masternodeService.getMasternode(ipAddress)
  }
}
