package com.xsn.explorer.models

import com.xsn.explorer.models.rpc.ScriptPubKey
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class TransactionVOUT(
    value: BigDecimal,
    n: Int,
    scriptPubKey: Option[ScriptPubKey] = None) {

  val address: Option[Address] = scriptPubKey.flatMap(_.addresses.headOption)
}

object TransactionVOUT {

  implicit val reads: Reads[TransactionVOUT] = {
    val builder = (__ \ 'value).read[BigDecimal] and
        (__ \ 'n).read[Int] and
        (__ \ 'scriptPubKey).readNullable[ScriptPubKey]

    builder.apply { (value, n, script) =>
      TransactionVOUT(value, n, script)
    }
  }
}
