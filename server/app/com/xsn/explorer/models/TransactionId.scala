package com.xsn.explorer.models

import com.xsn.explorer.models.base.WrappedString
import play.api.libs.json._

class TransactionId private (val string: String) extends AnyVal with WrappedString

object TransactionId {

  private val pattern = "^[a-f0-9]{64}$".r.pattern

  def from(string: String): Option[TransactionId] = {
    val lowercaseString = string.toLowerCase

    if (pattern.matcher(lowercaseString).matches()) {
      Some(new TransactionId(lowercaseString))
    } else {
      None
    }
  }

  implicit val reads: Reads[TransactionId] = Reads { json =>
    json.validate[String].flatMap { string =>
      from(string)
          .map(JsSuccess.apply(_))
          .getOrElse {
            JsError.apply("Invalid transaction")
          }
    }
  }
}
