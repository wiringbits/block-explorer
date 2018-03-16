package com.xsn.explorer.models

import com.xsn.explorer.models.base.WrappedInt
import play.api.libs.json.{JsPath, Reads}

case class Size(int: Int) extends AnyVal with WrappedInt

object Size {

  implicit val reads: Reads[Size] = JsPath.read[Int].map(Size.apply)
}
