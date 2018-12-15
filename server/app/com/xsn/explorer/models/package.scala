package com.xsn.explorer

import _root_.play.api.libs.json.{JsNumber, JsString, Writes}
import com.alexitc.playsonify.models.{WrappedInt, WrappedString}

package object models {

  implicit val stringWrites: Writes[WrappedString] = Writes { obj =>
    JsString(obj.string)
  }

  implicit val numberWrites: Writes[WrappedInt] = Writes { obj =>
    JsNumber(obj.int)
  }
}
