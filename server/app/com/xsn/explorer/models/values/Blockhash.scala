package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedString
import play.api.libs.json._

class Blockhash private (val string: String) extends AnyVal with WrappedString {

  def toBytesLE: List[Byte] = {
    string
      .grouped(2)
      .toList
      .reverse
      .map { hex =>
        Integer.parseInt(hex, 16).asInstanceOf[Byte]
      }
  }
}

object Blockhash {

  val Length = 64

  private val pattern = "^[a-f0-9]{64}$".r.pattern

  def from(string: String): Option[Blockhash] = {
    val lowercaseString = string.toLowerCase

    if (pattern.matcher(lowercaseString).matches()) {
      Some(new Blockhash(lowercaseString))
    } else {
      None
    }
  }

  implicit val reads: Reads[Blockhash] = Reads { json =>
    json.validate[String].flatMap { string =>
      from(string)
        .map(JsSuccess.apply(_))
        .getOrElse {
          JsError.apply("Invalid blockhash")
        }
    }
  }
}
