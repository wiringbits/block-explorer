package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedInt
import play.api.libs.json.{JsNumber, JsPath, Reads, Writes}

case class Confirmations(int: Int) extends AnyVal with WrappedInt

object Confirmations {

  implicit val reads: Reads[Confirmations] = JsPath.read[Int].map(Confirmations.apply)
  implicit val writes: Writes[Confirmations] = {
    Writes[Confirmations] { wrapped =>
      JsNumber(wrapped.int)
    }
  }
}
