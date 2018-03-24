package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.Address
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class ScriptPubKey(
    `type`: String,
    asm: String,
    addresses: List[Address]
)

object ScriptPubKey {

  implicit val reads: Reads[ScriptPubKey] = {
    val builder = (__ \ 'type).read[String] and
        (__ \ 'asm).read[String] and
        (__ \ 'addresses).readNullable[List[Address]].map(_ getOrElse List.empty)

    builder.apply { (t, asm, addresses) =>
      ScriptPubKey(t, asm, addresses)
    }
  }
}
