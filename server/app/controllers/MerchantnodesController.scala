package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.services.MerchantnodeService
import controllers.common.{Codecs, MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json.{Json}

class MerchantnodesController @Inject() (
    merchantnodeService: MerchantnodeService,
    cc: MyJsonControllerComponents
) extends MyJsonController(cc) {

  import Codecs._

  def get(offset: Int, limit: Int, ordering: String) = public { _ =>
    val paginatedQuery = PaginatedQuery(Offset(offset), Limit(limit))
    val orderingQuery = OrderingQuery(ordering)

    merchantnodeService
      .getMerchantnodes(paginatedQuery, orderingQuery)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getBy(ipAddress: String) = public { _ =>
    merchantnodeService
      .getMerchantnode(ipAddress)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }
}
