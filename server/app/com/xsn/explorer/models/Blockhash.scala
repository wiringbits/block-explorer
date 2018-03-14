package com.xsn.explorer.models

import play.api.libs.json._

class Blockhash private (val string: String) extends AnyVal

object Blockhash {
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

  implicit val writes: Writes[Blockhash] = Writes { obj => JsString(obj.string) }
}
