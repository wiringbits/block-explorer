package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedInt
import play.api.libs.json.{JsPath, Reads}

case class Height(int: Int) extends AnyVal with WrappedInt

object Height {

  implicit val reads: Reads[Height] = JsPath.read[Int].map(Height.apply)
}
