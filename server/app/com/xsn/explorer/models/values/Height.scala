package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedInt
import play.api.libs.json.{JsNumber, JsPath, Reads, Writes}

case class Height(int: Int) extends AnyVal with WrappedInt

object Height {

  implicit val reads: Reads[Height] = JsPath.read[Int].map(Height.apply)
  implicit val writes: Writes[Height] = {
    Writes[Height] { wrapped =>
      JsNumber(wrapped.int)
    }
  }
}
