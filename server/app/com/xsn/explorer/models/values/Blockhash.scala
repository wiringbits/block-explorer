package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedString
import javax.xml.bind.DatatypeConverter
import play.api.libs.json._

class Blockhash private (val string: String) extends AnyVal with WrappedString {

  def toBytesBE: List[Byte] = {
    string
      .grouped(2)
      .toList
      .map { hex =>
        Integer.parseInt(hex, 16).asInstanceOf[Byte]
      }
  }

  def toBytesLE: List[Byte] = {
    toBytesBE.reverse
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

  def fromBytesBE(bytes: Array[Byte]): Option[Blockhash] = {
    val string = DatatypeConverter.printHexBinary(bytes)
    from(string)
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
