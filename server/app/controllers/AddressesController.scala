package controllers

import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.models.Transaction
import com.xsn.explorer.services.{AddressService, TransactionService}
import com.xsn.explorer.util.Extensions.BigDecimalExt
import controllers.common.{Codecs, MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json._

class AddressesController @Inject() (
    addressService: AddressService,
    transactionService: TransactionService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Codecs._

  def getBy(address: String) = public { _ =>
    addressService.getBy(address)
  }

  def getTransactions(address: String, offset: Int, limit: Int, ordering: String) = public { _ =>
    val paginatedQuery = PaginatedQuery(Offset(offset), Limit(limit))

    transactionService.getTransactions(address, paginatedQuery, OrderingQuery(ordering))
  }

  def getLightWalletTransactions(address: String, limit: Int, before: Option[Long]) = public { _ =>
    transactionService.getLightWalletTransactions(
      address,
      before.getOrElse(java.lang.System.currentTimeMillis()),
      Limit(limit))
  }

  /**
   * Format to keep compatibility with the previous approach using the RPC api.
   */
  implicit private val writes: Writes[Transaction.Output] = Writes { obj =>
    val values = Map(
      "address" -> JsString(obj.address.string),
      "txid" -> JsString(obj.txid.string),
      "script" -> JsString(obj.script.string),
      "outputIndex" -> JsNumber(obj.index),
      "satoshis" -> JsNumber(BigDecimal(obj.value.toSatoshis))
    )

    JsObject.apply(values)
  }

  def getUnspentOutputs(address: String) = public { _ =>
    addressService.getUnspentOutputs(address)
  }
}
