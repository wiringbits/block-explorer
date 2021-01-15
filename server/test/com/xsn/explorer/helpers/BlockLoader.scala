package com.xsn.explorer.helpers

import java.io.File

import com.xsn.explorer.models._
import com.xsn.explorer.models.values.Blockhash
import play.api.libs.json.{JsValue, Json}
import io.scalaland.chimney.dsl._

import scala.util.Try

object BlockLoader {

  private val BasePath = "blocks"
  private val FullBlocksBasePath = "full-blocks"

  def get(blockhash: String): persisted.Block = {
    val rpcBlock = getRPC(blockhash)
    Converters.toPersistedBlock(rpcBlock)
  }

  def getWithTransactions(
      blockhash: String
  ): persisted.Block.HasTransactions = {
    getWithTransactions(blockhash, "xsn")
  }

  def getWithTransactions(
      blockhash: String,
      coin: String
  ): persisted.Block.HasTransactions = {
    val rpcBlock = getRPC(blockhash, coin)
    val block = Converters.toPersistedBlock(rpcBlock)
    val transactions = rpcBlock.transactions
      .map(_.string)
      .map(TransactionLoader.getWithValues(_, coin))
      .map(persisted.Transaction.fromRPC)
      .map(_._1)

    persisted.Block.HasTransactions(block, transactions)
  }

  def getWithTransactions(
      rpcBlock: rpc.Block.Canonical,
      coin: String = "xsn"
  ): persisted.Block.HasTransactions = {
    val block = Converters.toPersistedBlock(rpcBlock)
    val transactions = rpcBlock.transactions
      .map(_.string)
      .map(TransactionLoader.getWithValues(_, coin))
      .map(_.copy(blockhash = rpcBlock.hash))
      .map(persisted.Transaction.fromRPC)
      .map(_._1)

    persisted.Block.HasTransactions(block, transactions)
  }

  def getRPC(blockhash: String, coin: String = "xsn"): rpc.Block.Canonical = {
    val partial = json(blockhash, coin).as[rpc.Block.Canonical]
    cleanGenesisBlock(partial)
  }

  def getFullRPC(
      blockhash: String,
      coin: String = "xsn"
  ): rpc.Block.HasTransactions[rpc.TransactionVIN] = {
    val resource = s"$FullBlocksBasePath/$coin/$blockhash"
    jsonFromResource(resource).as[rpc.Block.HasTransactions[rpc.TransactionVIN]]
  }

  def getFullRPCWithValues(
      blockhash: String,
      coin: String = "xsn"
  ): rpc.Block.HasTransactions[rpc.TransactionVIN.HasValues] = {
    val block = json(blockhash, coin).as[rpc.Block.Canonical]
    val transactionsWithValues = block.transactions
      .map(_.toString)
      .map(TransactionLoader.getWithValues(_, coin))

    block
      .into[rpc.Block.HasTransactions[rpc.TransactionVIN.HasValues]]
      .withFieldConst(_.transactions, transactionsWithValues)
      .transform
  }

  def getFullRPCOpt(
      blockhash: String,
      coin: String = "xsn"
  ): Option[rpc.Block.HasTransactions[rpc.TransactionVIN]] = {
    Try(getFullRPC(blockhash, coin)).toOption
  }

  def json(blockhash: String, coin: String = "xsn"): JsValue = {
    val resource = s"$BasePath/$coin/$blockhash"
    jsonFromResource(resource)
  }

  def all(): List[persisted.Block] = {
    allRPC()
      .map(Converters.toPersistedBlock)
  }

  def allRPC(coin: String = "xsn"): List[rpc.Block.Canonical] = {
    val uri = getClass.getResource(s"/$BasePath/$coin")
    new File(uri.getPath)
      .listFiles()
      .toList
      .map(_.getName)
      .map(getRPC(_, coin))
  }

  def cleanGenesisBlock(block: rpc.Block.Canonical): rpc.Block.Canonical = {
    val genesisBlockhash: Blockhash =
      Blockhash
        .from(
          "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34"
        )
        .get

    Option(block)
      .filter(_.hash == genesisBlockhash)
      .map(_.copy(transactions = List.empty))
      .getOrElse(block)
  }

  private def jsonFromResource(resource: String): JsValue = {
    try {
      val json =
        scala.io.Source.fromResource(resource).getLines().mkString("\n")
      Json.parse(json)
    } catch {
      case _: Throwable =>
        throw new RuntimeException(s"Resource $resource not found")
    }
  }
}
