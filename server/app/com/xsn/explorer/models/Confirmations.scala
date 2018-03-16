package com.xsn.explorer.models

import com.xsn.explorer.models.base.WrappedInt
import play.api.libs.json.{JsPath, Reads}

case class Confirmations(int: Int) extends AnyVal with WrappedInt

object Confirmations {

  implicit val reads: Reads[Confirmations] = JsPath.read[Int].map(Confirmations.apply)
}
