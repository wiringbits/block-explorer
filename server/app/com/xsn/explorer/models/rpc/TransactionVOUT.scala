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
    val builder = (__ \ Symbol("value")).read[BigDecimal] and
      (__ \ Symbol("n")).read[Int] and
      (__ \ Symbol("scriptPubKey")).read[Option[ScriptPubKey]]

    builder.apply { (value, n, script) =>
      TransactionVOUT(value, n, script)
    }
  }
}
