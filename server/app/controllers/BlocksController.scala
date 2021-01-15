package controllers

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.models.LightWalletTransaction
import com.xsn.explorer.models.persisted.BlockHeader
import com.xsn.explorer.models.persisted.BlockInfo
import com.xsn.explorer.models.persisted.BlockInfoCodec
import com.xsn.explorer.models.values.Height
import com.xsn.explorer.services.{
  BlockService,
  TransactionRPCService,
  TransactionService
}
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject
import play.api.libs.json.{Json, Writes}

import scala.util.Try

class BlocksController @Inject() (
    blockService: BlockService,
    transactionService: TransactionService,
    transactionRPCService: TransactionRPCService,
    cc: MyJsonControllerComponents
) extends MyJsonController(cc) {

  import BlocksController._

  def getLatestBlocks() = public { _ =>
    blockService
      .getLatestBlocks()
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getBlocks(
      lastSeenHash: Option[String],
      limit: Int,
      orderingCondition: String
  ) = public { _ =>
    implicit val codec: Writes[BlockInfo] = BlockInfoCodec.completeWrites
    blockService
      .getBlocks(Limit(limit), lastSeenHash, orderingCondition)
      .toFutureOr
      .map { case (value, cacheable) =>
        val response = Ok(Json.toJson(value.data))
        if (cacheable) {
          response.withHeaders("Cache-Control" -> "public, max-age=31536000")
        } else {
          response.withHeaders("Cache-Control" -> "no-store")
        }
      }
      .toFuture
  }

  def getBlockHeaders(
      lastSeenHash: Option[String],
      limit: Int,
      orderingCondition: String
  ) = public { _ =>
    implicit val codec: Writes[BlockHeader] = BlockHeader.partialWrites
    blockService
      .getBlockHeaders(Limit(limit), lastSeenHash, orderingCondition)
      .toFutureOr
      .map { case (value, cacheable) =>
        val response = Ok(Json.toJson(value))
        if (cacheable) {
          response.withHeaders("Cache-Control" -> "public, max-age=31536000")
        } else {
          response.withHeaders("Cache-Control" -> "no-store")
        }
      }
      .toFuture
  }

  /** Try to retrieve a blockHeader by height, in case the query argument
    * is not a valid height, we assume it might be a blockhash and try to
    * retrieve the blockHeader by blockhash.
    */
  def getBlockHeader(query: String, includeFilter: Boolean) = public { _ =>
    implicit val codec: Writes[BlockHeader] = BlockHeader.completeWrites
    val (cache, resultF) = Try(query.toInt)
      .map(Height.apply)
      .map { value =>
        "no-store" -> blockService.getBlockHeader(value, includeFilter)
      }
      .getOrElse {
        "public, max-age=31536000" -> blockService.getBlockHeader(
          query,
          includeFilter
        )
      }

    resultF.toFutureOr.map { value =>
      val response = Ok(Json.toJson(value))
      response.withHeaders("Cache-Control" -> cache)
    }.toFuture
  }

  /** Try to retrieve a block by height, in case the query argument
    * is not a valid height, we assume it might be a blockhash and try to
    * retrieve the block by blockhash.
    */
  def getDetails(query: String) = public { _ =>
    Try(query.toInt)
      .map(Height.apply)
      .map(blockService.getDetails)
      .getOrElse(blockService.getDetails(query))
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getRawBlock(query: String) = public { _ =>
    Try(query.toInt)
      .map(Height.apply)
      .map(blockService.getRawBlock)
      .getOrElse(blockService.getRawBlock(query))
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getTransactionsV2(
      blockhash: String,
      limit: Int,
      lastSeenTxid: Option[String]
  ) = public { _ =>
    transactionService
      .getByBlockhash(blockhash, Limit(limit), lastSeenTxid)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getLightTransactionsV2(
      blockhash: String,
      limit: Int,
      lastSeenTxid: Option[String]
  ) = public { _ =>
    transactionService
      .getLightWalletTransactionsByBlockhash(
        blockhash,
        Limit(limit),
        lastSeenTxid
      )
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def estimateFee(nBlocks: Int) = public { _ =>
    blockService
      .estimateFee(nBlocks)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.toJson(value))
        response.withHeaders("Cache-Control" -> "public, max-age=60")
      }
      .toFuture
  }

  def getBlockLite(blockhash: String) = public { _ =>
    blockService
      .getBlockLite(blockhash)
      .toFutureOr
      .map {
        case (value, cacheable) => {
          val response = Ok(Json.toJson(value))
          if (cacheable) {
            response.withHeaders("Cache-Control" -> "public, max-age=31536000")
          } else {
            response.withHeaders("Cache-Control" -> "no-store")
          }
        }
      }
      .toFuture
  }

  def getHexEncodedBlock(blockhash: String) = public { _ =>
    blockService
      .getHexEncodedBlock(blockhash)
      .toFutureOr
      .map { value =>
        val response = Ok(Json.obj("hex" -> value))
        response.withHeaders("Cache-Control" -> "public, max-age=31536000")
      }
      .toFuture
  }

  def getLiteTransaction(height: Int, txindex: Int) = public { _ =>
    transactionRPCService
      .getTransactionLite(Height(height), txindex)
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
}

object BlocksController {

  implicit val inputWrites: Writes[LightWalletTransaction.Input] =
    (obj: LightWalletTransaction.Input) => {
      Json.obj(
        "txid" -> obj.txid,
        "index" -> obj.index
      )
    }

  implicit val outputWrites: Writes[LightWalletTransaction.Output] =
    (obj: LightWalletTransaction.Output) => {
      Json.obj(
        "index" -> obj.index,
        "value" -> obj.value,
        "addresses" -> obj.addresses,
        "script" -> obj.script.string
      )
    }
  implicit val lightWalletTransactionWrites: Writes[LightWalletTransaction] =
    (obj: LightWalletTransaction) => {
      Json.obj(
        "id" -> obj.id,
        "size" -> obj.size,
        "time" -> obj.time,
        "inputs" -> Json.toJson(obj.inputs),
        "outputs" -> Json.toJson(obj.outputs)
      )
    }
}
