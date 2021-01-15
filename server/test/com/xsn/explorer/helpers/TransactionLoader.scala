package com.xsn.explorer.helpers

import java.io.File

import com.xsn.explorer.models.rpc.{Transaction, TransactionVIN}
import play.api.libs.json.{JsValue, Json}

object TransactionLoader {

  private val BasePath = "transactions"

  def get(txid: String, coin: String = "xsn"): Transaction[TransactionVIN] = {
    json(txid, coin).as[Transaction[TransactionVIN]]
  }

  def getWithValues(
      txid: String,
      coin: String = "xsn"
  ): Transaction[TransactionVIN.HasValues] = {
    val plain = json(txid, coin).as[Transaction[TransactionVIN]]
    val newVIN = plain.vin.flatMap { vin =>
      get(vin.txid.string, coin).vout
        .find(_.n == vin.voutIndex)
        .flatMap { prev =>
          for {
            addresses <- prev.addresses
            scriptPubKey <- prev.scriptPubKey
          } yield vin.withValues(prev.value, addresses, scriptPubKey.hex)
        }
    }

    plain.copy(vin = newVIN)
  }

  def json(txid: String, coin: String = "xsn"): JsValue = {
    try {
      val resource = s"$BasePath/$coin/$txid"
      val json =
        scala.io.Source.fromResource(resource).getLines().mkString("\n")
      Json.parse(json)
    } catch {
      case _: Throwable =>
        throw new RuntimeException(s"Transaction $txid not found")
    }
  }

  def all(coin: String = "xsn"): List[Transaction[TransactionVIN]] = {
    val transactionsWithErrors = List(
      "6b984d317623fdb3f40e5d64a4236de33b9cb1de5f12a6abe2e8f242f6572655"
    )

    val uri = getClass.getResource(s"/$BasePath/$coin")
    new File(uri.getPath)
      .listFiles()
      .toList
      .map(_.getName)
      .filterNot(transactionsWithErrors.contains)
      .map(get(_, coin))
  }
}
