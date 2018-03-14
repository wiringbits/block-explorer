package com.xsn.explorer.models

import play.api.libs.json._

class Address private (val string: String) extends AnyVal

object Address {

  private val pattern = "^[a-zA-Z0-9]{34}$".r.pattern

  def from(string: String): Option[Address] = {
    if (pattern.matcher(string).matches()) {
      Some(new Address(string))
    } else {
      None
    }
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

  implicit val writes: Writes[Address] = Writes { obj =>
    JsString(obj.string)
  }
}
