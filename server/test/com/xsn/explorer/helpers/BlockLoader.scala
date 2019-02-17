package com.xsn.explorer.helpers

import java.io.File

import com.xsn.explorer.models._
import com.xsn.explorer.models.values.Blockhash
import play.api.libs.json.{JsValue, Json}

object BlockLoader {

  private val BasePath = "blocks"

  def get(blockhash: String): persisted.Block = {
    val rpcBlock = getRPC(blockhash)
    Converters.toPersistedBlock(rpcBlock)
  }

  def getRPC(blockhash: String): rpc.Block = {
    val partial = json(blockhash).as[rpc.Block]
    cleanGenesisBlock(partial)
  }

  def json(blockhash: String): JsValue = {
    try {
      val resource = s"$BasePath/$blockhash"
      val json = scala.io.Source.fromResource(resource).getLines().mkString("\n")
      Json.parse(json)
    } catch {
      case _: Throwable => throw new RuntimeException(s"Block $blockhash not found")
    }
  }

  def all(): List[persisted.Block] = {
    allRPC()
        .map(Converters.toPersistedBlock)
  }

  def allRPC(): List[rpc.Block] = {
    val uri = getClass.getResource(s"/$BasePath")
    new File(uri.getPath)
        .listFiles()
        .toList
        .map(_.getName)
        .map(getRPC)
  }

  def cleanGenesisBlock(block: rpc.Block): rpc.Block = {
    val genesisBlockhash: Blockhash = Blockhash.from("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34").get

    Option(block)
      .filter(_.hash == genesisBlockhash)
      .map(_.copy(transactions = List.empty))
      .getOrElse(block)
  }
}
