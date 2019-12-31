package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.models.request.SendRawTransactionRequest
import com.xsn.explorer.services.TransactionRPCService
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json.Json

class TransactionsController @Inject()(transactionRPCService: TransactionRPCService, cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Context._

  def getTransaction(txid: String) = public { _ =>
    transactionRPCService.getTransactionDetails(txid)
    .toFutureOr
    .map {
      value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
    }
    .toFuture
  }

  def getRawTransaction(txid: String) = public { _ =>
    transactionRPCService.getRawTransaction(txid)
    .toFutureOr
    .map {
      value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
    }
    .toFuture
  }

  def sendRawTransaction() = publicInput { ctx: HasModel[SendRawTransactionRequest] =>
    transactionRPCService.sendRawTransaction(ctx.model.hex)
  }

  def getTransactionLite(txid: String) = public { _ =>
    transactionRPCService
      .getTransactionLite(txid)
      .toFutureOr
      .map {
        case (value, cacheable) =>
          val response = Ok(value)
          if (cacheable) {
            response.withHeaders("Cache-Control" -> "public, max-age=31536000")
          } else {
            response.withHeaders("Cache-Control" -> "no-store")
          }
      }
      .toFuture
  }

  def getTransactionUtxoByIndex(txid: String, index: Int, includeMempool: Boolean) = public { _ =>
    transactionRPCService.getTransactionUtxoByIndex(txid, index, includeMempool)
    .toFutureOr
    .map {
      value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
    }
    .toFuture
  }
}
