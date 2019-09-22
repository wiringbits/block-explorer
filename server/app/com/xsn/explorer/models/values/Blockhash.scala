package com.xsn.explorer.models.values

import play.api.libs.json._

class Blockhash private (val string: String) extends AnyVal with SHA256Value

object Blockhash {

  def from(string: String): Option[Blockhash] = {
    SHA256Value.from(string).map(x => new Blockhash(x.string))
  }

  def fromBytesBE(bytes: Array[Byte]): Option[Blockhash] = {
    SHA256Value.fromBytesBE(bytes).map(x => new Blockhash(x.string))
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
