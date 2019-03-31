package com.xsn.explorer.helpers

import java.io.File

import com.xsn.explorer.models.rpc.{Transaction, TransactionVIN}
import play.api.libs.json.{JsValue, Json}

object TransactionLoader {

  private val BasePath = "transactions"

  def get(txid: String): Transaction[TransactionVIN] = {
    json(txid).as[Transaction[TransactionVIN]]
  }

  def getWithValues(txid: String): Transaction[TransactionVIN.HasValues] = {
    val plain = json(txid).as[Transaction[TransactionVIN]]
    val newVIN = plain.vin.flatMap { vin =>
      get(vin.txid.string)
          .vout
          .find(_.n == vin.voutIndex)
          .flatMap { prev =>
            prev.address.map { vin.withValues(prev.value, _) }
          }
    }

    plain.copy(vin = newVIN)
  }

  def json(txid: String): JsValue = {
    try {
      val resource = s"$BasePath/$txid"
      val json = scala.io.Source.fromResource(resource).getLines().mkString("\n")
      Json.parse(json)
    } catch {
      case _: Throwable => throw new RuntimeException(s"Transaction $txid not found")
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
