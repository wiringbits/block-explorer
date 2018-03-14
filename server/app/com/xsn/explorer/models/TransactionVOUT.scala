package com.xsn.explorer.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsObject, Reads, __}

case class TransactionVOUT(
    value: BigDecimal,
    n: Int,
    scriptPubKeyType: String,
    address: Option[Address])

object TransactionVOUT {

  implicit val reads: Reads[TransactionVOUT] = {
    val builder = (__ \ 'value).read[BigDecimal] and
        (__ \ 'n).read[Int] and
        (__ \ 'scriptPubKey).read[JsObject].map { json =>
          val t = (json \ "type").as[String]
          val a = (json \ "addresses")
              .asOpt[List[String]]
              .flatMap(_.headOption)
              .flatMap(Address.from)

          (t, a)
        }

    builder.apply { (value, n, tuple) =>
      val (scriptType, address) = tuple
      TransactionVOUT(value, n, scriptType, address)
    }
  }
}
