package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.models.request.{SendRawTransactionRequest, TposContractsEncodeRequest}
import com.xsn.explorer.services.{BlockService, TransactionRPCService, TransactionService}
import com.alexitc.playsonify.models.pagination.Limit
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import org.scalactic.Good

import javax.inject.Inject
import play.api.libs.json.Json

import scala.concurrent.Future

class TransactionsController @Inject() (
    transactionRPCService: TransactionRPCService,
    transactionService: TransactionService,
    blockService: BlockService,
    cc: MyJsonControllerComponents
) extends MyJsonController(cc) {

  import Context._

  def getTransactions(
      limit: Int,
      lastSeenTxid: Option[String],
      orderingCondition: String,
      includeZeroValueTransactions: Boolean
  ) = public { _ =>
    transactionService
      .get(
        Limit(limit),
        lastSeenTxid,
        orderingCondition,
        includeZeroValueTransactions
      )
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getTransaction(txid: String) = public { _ =>
    transactionRPCService
      .getTransactionDetails(txid)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=31536000")
      }
      .toFuture
  }

  def getRawTransaction(txid: String) = public { _ =>
    transactionRPCService
      .getRawTransaction(txid)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=31536000")
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
      .map { case (value, cacheable) =>
        val response = Ok(value)
        if (cacheable) {
          response.withHeaders("Cache-Control" -> "public, max-age=31536000")
        } else {
          response.withHeaders("Cache-Control" -> "public, max-age=60")
        }
      }
      .toFuture
  }

  def getTransactionUtxoByIndex(
      txid: String,
      index: Int,
      includeMempool: Boolean
  ) = public { _ =>
    transactionRPCService
      .getTransactionUtxoByIndex(txid, index, includeMempool)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=31536000")
      }
      .toFuture
  }

  def getSpendingTransaction(txid: String, index: Int) = public { _ =>
    val result = transactionService.getSpendingTransaction(txid, index).toFutureOr.flatMap {
      case Some(spendingTransaction) =>
        for {
          blockHeader <- blockService
            .getBlockHeader(spendingTransaction.blockhash.toString, includeFilter = false)
            .toFutureOr

          rawTransaction <- transactionRPCService.getRawTransaction(spendingTransaction.id.string).toFutureOr

          response = Json.obj("rawTransaction" -> rawTransaction, "height" -> blockHeader.height.int)
        } yield Ok(Json.toJson(response))

      case None =>
        Future.successful(Good(Ok(Json.parse("{}")))).toFutureOr
    }

    result.toFuture
  }

  def encodeTPOSContract() = publicInput { ctx: HasModel[TposContractsEncodeRequest] =>
    transactionRPCService
      .encodeTPOSContract(
        tposAddress = ctx.model.tposAddress,
        merchantAddress = ctx.model.merchantAddress,
        commission = ctx.model.commission,
        signature = ctx.model.signature
      )
      .toFutureOr
      .map { value =>
        val response = Ok(value)
        response.withHeaders("Cache-Control" -> "no-store")
      }
      .toFuture
  }
}
