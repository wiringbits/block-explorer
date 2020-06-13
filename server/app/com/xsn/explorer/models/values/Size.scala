package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedInt
import play.api.libs.json.{JsNumber, JsPath, Reads, Writes}

case class Size(int: Int) extends AnyVal with WrappedInt

object Size {

  implicit val reads: Reads[Size] = JsPath.read[Int].map(Size.apply)
  implicit val writes: Writes[Size] = {
    Writes[Size] { wrapped =>
      JsNumber(wrapped.int)
    }
  }
}
