package com.xsn.explorer.models.values

import play.api.libs.json._

class TransactionId private (val string: String) extends AnyVal with SHA256Value

object TransactionId {

  def from(string: String): Option[TransactionId] = {
    SHA256Value.from(string).map(x => new TransactionId(x.string))
  }

  def fromBytesBE(bytes: Array[Byte]): Option[TransactionId] = {
    SHA256Value.fromBytesBE(bytes).map(x => new TransactionId(x.string))
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
