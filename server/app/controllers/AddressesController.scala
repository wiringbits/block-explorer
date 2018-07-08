package controllers

import javax.inject.Inject

import com.alexitc.playsonify.models.{Limit, Offset, OrderingQuery, PaginatedQuery}
import com.xsn.explorer.models.Transaction
import com.xsn.explorer.services.{AddressService, TransactionService}
import com.xsn.explorer.util.Extensions.BigDecimalExt
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import play.api.libs.json._

class AddressesController @Inject() (
    addressService: AddressService,
    transactionService: TransactionService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getBy(address: String) = publicNoInput { _ =>
    addressService.getBy(address)
  }

  def getTransactions(address: String, offset: Int, limit: Int, ordering: String) = publicNoInput { _ =>
    val paginatedQuery = PaginatedQuery(Offset(offset), Limit(limit))

    transactionService.getTransactions(address, paginatedQuery, OrderingQuery(ordering))
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

  def getUnspentOutputs(address: String) = publicNoInput { _ =>
    addressService.getUnspentOutputs(address)
  }
}
