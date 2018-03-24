package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.Address
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class ScriptPubKey(
    `type`: String,
    asm: String,
    addresses: List[Address]) {

  /**
   * Parse addresses from a TPoS contract transaction.
   *
   * @return (owner address, merchant address)
   */
  def getTPoSAddresses: Option[(Address, Address)] = {
    // "asm": "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99"
    Option(asm)
        .map(_ split " ")
        .filter(_.size == 4)
        .map(_.toList)
        .flatMap {
          case op :: owner :: merchant :: commission if op == "OP_RETURN" =>
            for {
              ownerAddress <- Address.fromHex(owner)
              merchantAddress <- Address.fromHex(merchant)
            } yield (ownerAddress, merchantAddress)

          case _ => None
        }
  }
}

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
