package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{Address, TransactionId}
import play.api.libs.json.{Json, Writes}

import scala.util.Try

case class Merchantnode(
    pubkey: String,
    txid: TransactionId,
    ip: String,
    protocol: String,
    status: String,
    activeSeconds: Long,
    lastSeen: Long,
    payee: Address
)

object Merchantnode {

  implicit val writes: Writes[Merchantnode] = Json.writes[Merchantnode]

  /**
   * The RPC server give us a map like this one:
   *
   * ```
   * {
   *   "36383165613065623435373332353634303664656666653535303735616465343966306433363232" : "ENABLED 70209 XdojZTbTDmKJmoyqqTCx3YRiAjTpRTmw5Z 8b5c4b85456d05b5039e510b6c09e81a6436de60b0be8ef055f551afaa712fc2 1598984070  2265879 45.32.137.108:62583"
   * }
   * ```
   *
   * The key is the public key
   */
  def fromMap(values: Map[String, String]): List[Merchantnode] = {
    values
      .map {
        case (key, value) =>
          val list = value.split(" ").map(_.trim).filter(_.nonEmpty).toList
          parseValues(key, list)
      }
      .toList
      .flatten
  }

  private def parseValues(key: String, values: List[String]): Option[Merchantnode] = values match {
    case status :: protocol :: payee :: tposTxid :: lastSeen :: activeSeconds :: ip :: _ =>
      for {
        txid <- TransactionId.from(tposTxid)
        payee <- Address.from(payee)
        lastSeen <- Try(lastSeen.toLong).toOption
        activeSeconds <- Try(activeSeconds.toLong).toOption
      } yield Merchantnode(
        pubkey = key,
        txid = txid,
        status = status,
        protocol = protocol,
        payee = payee,
        ip = ip,
        lastSeen = lastSeen,
        activeSeconds = activeSeconds
      )

    case _ => None
  }
}
