package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.errors.{BlockNotFoundError, TransactionError}
import com.xsn.explorer.helpers.DataGenerator.randomTPoSContract
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.rpc.{Block, Transaction, TransactionVIN}
import com.xsn.explorer.models.values.{
  Address,
  Blockhash,
  Height,
  TransactionId
}
import org.scalactic.{Good, One, Or}
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.util.Try

class FileBasedXSNService extends DummyXSNService {

  private lazy val blockMap = BlockLoader
    .allRPC()
    .map { block =>
      block.hash -> block
    }
    .toMap

  private lazy val transactionMap = TransactionLoader
    .all()
    .map { tx =>
      tx.id -> tx
    }
    .toMap

  override def getBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[Block.Canonical] = {
    val maybe = blockMap.get(blockhash)
    val result = Or.from(maybe, One(BlockNotFoundError))
    Future.successful(result)
  }

  override def getFullBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[Block.HasTransactions[TransactionVIN]] = {
    val maybe = BlockLoader.getFullRPCOpt(blockhash.string)
    val result = Or.from(maybe, One(BlockNotFoundError))
    Future.successful(result)
  }

  override def getRawBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[JsValue] = {
    val result = BlockLoader.json(blockhash.string)
    Future.successful(Good(result))
  }

  override def getBlockhash(
      height: Height
  ): FutureApplicationResult[Blockhash] = {
    val maybe = blockMap.collectFirst {
      case (_, block) if block.height == height => block.hash
    }

    val result = Or.from(maybe, One(BlockNotFoundError))
    Future.successful(result)
  }

  override def getLatestBlock(): FutureApplicationResult[Block.Canonical] = {
    val block = blockMap.values.maxBy(_.height.int)
    Future.successful(Good(block))
  }

  override def getTransaction(
      txid: TransactionId
  ): FutureApplicationResult[Transaction[TransactionVIN]] = {
    val maybe = transactionMap.get(txid)
    val result = Or.from(maybe, One(TransactionError.NotFound(txid)))
    Future.successful(result)
  }

  override def getRawTransaction(
      txid: TransactionId
  ): FutureApplicationResult[JsValue] = {
    val maybe = Try(TransactionLoader.json(txid.string)).toOption
    val result = Or.from(maybe, One(TransactionError.NotFound(txid)))
    Future.successful(result)
  }

  override def isTPoSContract(
      txid: TransactionId
  ): FutureApplicationResult[Boolean] = Future.successful(Good(true))

  override def encodeTPOSContract(
      tposAddress: Address,
      merchantAddress: Address,
      commission: Int,
      signature: String
  ): FutureApplicationResult[String] = {
    val tposEncoded =
      "020000000a001976a91495cf859d7a40c5d7fded2a03cb8d7dcf307eab1188ac1976a914a7e2ba4e0d91273d686f446fa04ca5fe800d452d88ac41201f2d052fb372248f89f9f2c9106be9a670d5538c01e4f39215c92717b847d3ea2466e7d1d88010ff98996913ed024dde8ebc860984f7806e5619c88cabf2ef06"
    Future.successful(Good(tposEncoded))
  }

  override def getTPoSContractDetails(
      transactionId: TransactionId
  ): FutureApplicationResult[TPoSContract.Details] = {
    val contractDetails = randomTPoSContract(txid = transactionId).details
    Future.successful(Good(contractDetails))
  }
}
