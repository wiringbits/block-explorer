package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.{Address, HexString}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class ScriptPubKey(
    `type`: String,
    asm: String,
    hex: HexString,
    addresses: List[Address]) {

  /**
   * Parse addresses from a TPoS contract transaction.
   *
   * @return (owner address, merchant address)
   */
  def getTPoSAddresses: Option[(Address, Address)] = {
    /**
     * expected:
     * - "asm": "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99"
     *
     * new format:
     *- "asm": "OP_RETURN 586a55587938507a55464d78534c37594135767866574a587365746b354d5638676f 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99 1f60a6a385a4e5163ffef65dd873f17452bb0d9f89da701ffcc5a0f72287273c0571485c29123fef880d2d8169cfdb884bf95a18a0b36461517acda390ce4cf441"
     */
    Option(asm)
        .map(_ split " ")
        .filter(_.size >= 4) // relax size check
        .map(_.toList)
        .flatMap {
          case op :: owner :: merchant :: _ if op == "OP_RETURN" =>
            for {
              ownerAddress <- Address.fromHex(owner)
              merchantAddress <- Address.fromHex(merchant)
            } yield (ownerAddress, merchantAddress)

          case _ => None
        }
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
