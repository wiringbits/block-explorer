package com.xsn.explorer.helpers

import com.xsn.explorer.models.rpc.Transaction
import play.api.libs.json.{JsValue, Json}

object TransactionLoader {

  private val BasePath = "transactions"

  def get(txid: String): Transaction = {
    json(txid).as[Transaction]
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
}
