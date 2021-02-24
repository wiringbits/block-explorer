package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.Address
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class TransactionVOUT(
    value: BigDecimal,
    n: Int,
    scriptPubKey: Option[ScriptPubKey] = None
) {

  val addresses: Option[List[Address]] = scriptPubKey.map(_.addresses)
}

object TransactionVOUT {

  implicit val reads: Reads[TransactionVOUT] = {
    val builder = (__ \ 'value).read[BigDecimal] and
      (__ \ 'n).read[Int] and
      (__ \ 'scriptPubKey).read[Option[ScriptPubKey]]

    builder.apply { (value, n, script) =>
      TransactionVOUT(value, n, script)
    }
  }
}
