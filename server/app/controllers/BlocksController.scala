package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.models.LightWalletTransaction
import com.xsn.explorer.models.values.Height
import com.xsn.explorer.services.{BlockService, TransactionService}
import controllers.common.{Codecs, MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json.{Json, Writes}

import scala.util.Try

class BlocksController @Inject()(
    blockService: BlockService,
    transactionService: TransactionService,
    cc: MyJsonControllerComponents
) extends MyJsonController(cc) {

  import BlocksController._
  import Codecs._

  def getLatestBlocks() = public { _ =>
    blockService.getLatestBlocks()
  }

  def getBlockHeaders(lastSeenHash: Option[String], limit: Int, orderingCondition: String) = public { _ =>
    blockService
      .getBlockHeaders(Limit(limit), lastSeenHash, orderingCondition)
      .toFutureOr
      .map {
        case (value, cacheable) =>
          val response = Ok(Json.toJson(value))
          if (cacheable) {
            response.withHeaders("Cache-Control" -> "public, max-age=31536000")
          } else {
            response.withHeaders("Cache-Control" -> "no-store")
          }
      }
      .toFuture
  }

  /**
   * Try to retrieve a blockHeader by height, in case the query argument
   * is not a valid height, we assume it might be a blockhash and try to
   * retrieve the blockHeader by blockhash.
   */
  def getBlockHeader(query: String, includeFilter: Boolean) = public { _ =>
    val (cache, resultF) = Try(query.toInt)
      .map(Height.apply)
      .map { value =>
        "no-store" -> blockService.getBlockHeader(value, includeFilter)
      }
      .getOrElse {
        "public, max-age=31536000" -> blockService.getBlockHeader(query, includeFilter)
      }

    resultF.toFutureOr.map { value =>
      val response = Ok(Json.toJson(value))
      response.withHeaders("Cache-Control" -> cache)
    }.toFuture
  }

  /**
   * Try to retrieve a block by height, in case the query argument
   * is not a valid height, we assume it might be a blockhash and try to
   * retrieve the block by blockhash.
   */
  def getDetails(query: String) = public { _ =>
    Try(query.toInt)
      .map(Height.apply)
      .map(blockService.getDetails)
      .getOrElse(blockService.getDetails(query))
  }

  def getRawBlock(query: String) = public { _ =>
    Try(query.toInt)
      .map(Height.apply)
      .map(blockService.getRawBlock)
      .getOrElse(blockService.getRawBlock(query))
  }

  def getTransactions(blockhash: String, offset: Int, limit: Int, orderBy: String) = public { _ =>
    val query = PaginatedQuery(Offset(offset), Limit(limit))
    val ordering = OrderingQuery(orderBy)
    transactionService.getByBlockhash(blockhash, query, ordering)
  }

  def getTransactionsV2(blockhash: String, limit: Int, lastSeenTxid: Option[String]) = public { _ =>
    transactionService.getByBlockhash(blockhash, Limit(limit), lastSeenTxid)
  }

  def getLightTransactionsV2(blockhash: String, limit: Int, lastSeenTxid: Option[String]) = public { _ =>
    transactionService.getLightWalletTransactionsByBlockhash(blockhash, Limit(limit), lastSeenTxid)
  }

  def estimateFee(nBlocks: Int) = public { _ =>
    blockService.estimateFee(nBlocks)
  }
}

object BlocksController {

  implicit val inputWrites: Writes[LightWalletTransaction.Input] = (obj: LightWalletTransaction.Input) => {
    Json.obj(
      "txid" -> obj.txid,
      "index" -> obj.index
    )
  }

  implicit val outputWrites: Writes[LightWalletTransaction.Output] = (obj: LightWalletTransaction.Output) => {
    Json.obj(
      "index" -> obj.index,
      "value" -> obj.value,
      "addresses" -> obj.addresses,
      "script" -> obj.script.string
    )
  }
  implicit val lightWalletTransactionWrites: Writes[LightWalletTransaction] = (obj: LightWalletTransaction) => {
    Json.obj(
      "id" -> obj.id,
      "size" -> obj.size,
      "time" -> obj.time,
      "inputs" -> Json.toJson(obj.inputs),
      "outputs" -> Json.toJson(obj.outputs)
    )
  }
}
