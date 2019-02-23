package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedString
import javax.xml.bind.DatatypeConverter
import play.api.libs.json._

import scala.util.Try

class Address private (val string: String) extends AnyVal with WrappedString

object Address {

  private val pattern = "^[a-zA-Z0-9]{22,64}$".r.pattern

  def from(string: String): Option[Address] = {
    if (pattern.matcher(string).matches()) {
      Some(new Address(string))
    } else {
      None
    }
  }

  def fromHex(hex: String): Option[Address] = {
    Try { DatatypeConverter.parseHexBinary(hex) }
        .map { bytes => new String(bytes) }
        .toOption
        .flatMap(from)
  }

  implicit val reads: Reads[Address] = Reads { json =>
    json.validate[String].flatMap { string =>
      from(string)
          .map(JsSuccess.apply(_))
          .getOrElse {
            JsError.apply("Invalid address")
          }
    }
  }
}
