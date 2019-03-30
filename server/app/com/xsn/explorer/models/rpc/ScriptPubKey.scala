package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.values.{Address, HexString}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class ScriptPubKey(
    `type`: String,
    asm: String,
    hex: HexString,
    addresses: List[Address]) {

  /**
   * Get TPoS contract details if available
   */
  def getTPoSContractDetails: Option[TPoSContract.Details] = {
    TPoSContract.Details.fromOutputScriptASM(asm)
  }
}

object ScriptPubKey {

  implicit val reads: Reads[Option[ScriptPubKey]] = {
    val builder = (__ \ 'type).read[String] and
        (__ \ 'asm).read[String] and
        (__ \ 'hex).read[String].map(HexString.from) and
        (__ \ 'addresses).readNullable[List[Address]].map(_ getOrElse List.empty)

    builder.apply { (t, asm, hexString, addresses) =>
      for {
        hex <- hexString
      } yield ScriptPubKey(t, asm = asm, hex = hex, addresses = addresses)
    }
  }
}
