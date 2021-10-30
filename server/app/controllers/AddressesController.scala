package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.services.{AddressService, TPoSContractService, TransactionService}
import com.xsn.explorer.util.Extensions.BigDecimalExt
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json._

class AddressesController @Inject() (
    addressService: AddressService,
    transactionService: TransactionService,
    tposContractService: TPoSContractService,
    cc: MyJsonControllerComponents
) extends MyJsonController(cc) {

  def getBy(address: String) = public { _ =>
    addressService
      .getBy(address)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=120")
      }
      .toFuture
  }

  def getLightWalletTransactions(
      address: String,
      limit: Int,
      lastSeenTxid: Option[String],
      orderingCondition: String
  ) =
    public { _ =>
      transactionService
        .getLightWalletTransactions(
          address,
          Limit(limit),
          lastSeenTxid,
          orderingCondition
        )
        .toFutureOr
        .map { value =>
          val response = Ok(Json.toJson(value))
          response.withHeaders("Cache-Control" -> "public, max-age=60")
        }
        .toFuture
    }

  def getTransactions(
      address: String,
      limit: Int,
      lastSeenTxid: Option[String],
      orderingCondition: String
  ) =
    public { _ =>
      transactionService
        .getByAddress(address, Limit(limit), lastSeenTxid, orderingCondition)
        .toFutureOr
        .map { value =>
          val response = Ok(Json.toJson(value))
          response.withHeaders("Cache-Control" -> "public, max-age=60")
        }
        .toFuture
    }

  /** Format to keep compatibility with the previous approach using the RPC api.
    */
  implicit private val writes: Writes[Transaction.Output] = Writes { obj =>
    val address = obj.addresses.headOption
      .map(_.string)
      .map(JsString.apply)
      .getOrElse(JsNull)

    val values = Map(
      "address" -> address, // Keep compatibility with the legacy API
      "addresses" -> JsArray(obj.addresses.map(_.string).map(JsString.apply)),
      "txid" -> JsString(obj.txid.string),
      "script" -> JsString(obj.script.string),
      "outputIndex" -> JsNumber(obj.index),
      "satoshis" -> JsNumber(BigDecimal(obj.value.toSatoshis))
    )

    JsObject.apply(values)
  }

  def getUnspentOutputs(address: String) = public { _ =>
    addressService
      .getUnspentOutputs(address)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getTPoSContracts(address: String) = public { _ =>
    tposContractService
      .getBy(address)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=120")
      }
      .toFuture
  }
}
