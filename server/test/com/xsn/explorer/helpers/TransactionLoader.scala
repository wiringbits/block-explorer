package com.xsn.explorer.helpers

import java.io.File

import com.xsn.explorer.models.rpc.{Transaction, TransactionVIN}
import play.api.libs.json.{JsValue, Json}

object TransactionLoader {

  private val BasePath = "transactions"

  def get(txid: String): Transaction[TransactionVIN] = {
    json(txid).as[Transaction[TransactionVIN]]
  }

  def json(txid: String): JsValue = {
    try {
      val resource = s"$BasePath/$txid"
      val json = scala.io.Source.fromResource(resource).getLines().mkString("\n")
      Json.parse(json)
    } catch {
      case _ => throw new RuntimeException(s"Transaction $txid not found")
    }
  }

  def all(): List[Transaction[TransactionVIN]] = {
    val uri = getClass.getResource(s"/$BasePath")
    new File(uri.getPath)
        .listFiles()
        .toList
        .map(_.getName)
        .map(get)
  }
}
