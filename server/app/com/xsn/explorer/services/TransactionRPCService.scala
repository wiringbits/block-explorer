package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.models.TransactionDetails
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.validators.TransactionIdValidator
import com.xsn.explorer.util.Extensions.BigDecimalExt
import javax.inject.Inject
import org.scalactic.{Bad, Good, One, Or}
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class TransactionRPCService @Inject()(
    transactionIdValidator: TransactionIdValidator,
    transactionCollectorService: TransactionCollectorService,
    xsnService: XSNService
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getRawTransaction(txidString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      txid <- transactionIdValidator.validate(txidString).toFutureOr
      transaction <- xsnService.getRawTransaction(txid).toFutureOr
    } yield transaction

    result.toFuture
  }

  def getTransactionDetails(txidString: String): FutureApplicationResult[TransactionDetails] = {
    val result = for {
      txid <- transactionIdValidator.validate(txidString).toFutureOr
      transaction <- xsnService.getTransaction(txid).toFutureOr
      vin <- transactionCollectorService.getRPCTransactionVIN(transaction.vin).toFutureOr
    } yield TransactionDetails.from(transaction.copy(vin = vin))

    result.toFuture
  }

  def sendRawTransaction(hexString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      hex <- Or.from(HexString.from(hexString), One(TransactionError.InvalidRawTransaction)).toFutureOr
      txid <- xsnService.sendRawTransaction(hex).toFutureOr
    } yield Json.obj("txid" -> JsString(txid))

    result.toFuture
  }

  private def canCacheResult(latestKnownBlock: Block.Canonical, height: Height): Boolean = {
    height.int + 20 < latestKnownBlock.height.int // there are at least 20 more blocks (unlikely to occur rollbacks)
  }

  def getTransactionLite(txidString: String): FutureApplicationResult[(JsValue, Boolean)] = {
    val result = for {
      latestBlock <- xsnService.getLatestBlock().toFutureOr
      txid <- transactionIdValidator.validate(txidString).toFutureOr
      jsonTransaction <- xsnService.getRawTransaction(txid).toFutureOr
      hex <- Or.from((jsonTransaction \ "hex").asOpt[String], One(TransactionError.NotFound(txid))).toFutureOr
      blockhash <- Or
        .from((jsonTransaction \ "blockhash").asOpt[Blockhash], One(TransactionError.NotFound(txid)))
        .toFutureOr
      block <- xsnService.getBlock(blockhash).toFutureOr
      index = block.transactions.indexOf(txid)
      height = block.height
    } yield (
      Json.obj("hex" -> hex, "blockhash" -> blockhash, "index" -> index, "height" -> height),
      canCacheResult(latestBlock, height)
    )

    result.toFuture
  }

  def getTransactionLite(height: Height, txindex: Int): FutureApplicationResult[(JsValue, Boolean)] = {
    val result = for {
      latestBlock <- xsnService.getLatestBlock().toFutureOr
      blockhash <- xsnService.getBlockhash(height).toFutureOr
      block <- xsnService.getBlock(blockhash).toFutureOr
      txid <- Future
        .successful(
          block.transactions
            .lift(txindex)
            .map(Good(_))
            .getOrElse(Bad(One(TransactionError.IndexNotFound(height, txindex))))
        )
        .toFutureOr
      jsonTransaction <- xsnService.getRawTransaction(txid).toFutureOr
      hex <- Or.from((jsonTransaction \ "hex").asOpt[String], One(TransactionError.NotFound(txid))).toFutureOr
    } yield (
      Json.obj("hex" -> hex, "blockhash" -> blockhash) -> canCacheResult(latestBlock, height)
    )

    result.toFuture
  }

  def getTransactionUtxoByIndex(
      txidString: String,
      index: Int,
      includeMempool: Boolean
  ): FutureApplicationResult[JsValue] = {
    val result = for {
      txid <- transactionIdValidator.validate(txidString).toFutureOr
      jsvalue <- xsnService.getTxOut(txid, index, includeMempool).toFutureOr
      value <- Or.from((jsvalue \ "value").asOpt[BigDecimal], One(TransactionError.NotFound(txid))).toFutureOr
      jsonScriptHex <- Or
        .from((jsvalue \ "scriptPubKey" \ "hex").asOpt[String], One(TransactionError.NotFound(txid)))
        .toFutureOr
    } yield Json.obj("value" -> value.toSatoshis.toString, "script" -> jsonScriptHex)

    result.toFuture
  }
}
