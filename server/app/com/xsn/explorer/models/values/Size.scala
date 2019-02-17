package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedInt
import play.api.libs.json.{JsPath, Reads}

case class Size(int: Int) extends AnyVal with WrappedInt

object Size {

  implicit val reads: Reads[Size] = JsPath.read[Int].map(Size.apply)
}
