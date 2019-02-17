package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.values.{Address, TransactionId}
import play.api.libs.json.{Json, Writes}

import scala.util.Try

case class Masternode(
    txid: TransactionId,
    ip: String,
    protocol: String,
    status: String,
    activeSeconds: Long,
    lastSeen: Long,
    payee: Address
)

object Masternode {

  implicit val writes: Writes[Masternode] = Json.writes[Masternode]

  /**
   * The RPC server give us a map like this one:
   *
   * ```
   * {
   *   "47b46ba99c760eeb6f443e5b6228d5dfeeac1cd5eec5fb9a79471af14c4c4c00-1": "  ENABLED 70208 Xo27xzC57FonGesBDqyoqoFZ9kLhy946Be 1524348946  1146950 1524252156  63199 45.77.63.186:62583"
   * }
   * ```
   *
   * The key is the transaction id used to send the funds to the masternode and the value a console formatted string with the values.
   * Note that the transaction id ends with `-x` where x is a number.
   */
  def fromMap(values: Map[String, String]): List[Masternode] = {
    values
        .map { case (key, value) =>
          val list = value.split(" ").map(_.trim).filter(_.nonEmpty).toList
          parseValues(key, list)
        }
        .toList
        .flatten
  }

  private def parseTxid(key: String): Option[TransactionId] = {
    key
        .split("\\,")
        .headOption
        .flatMap(_.split("\\(").lift(1))
        .flatMap(TransactionId.from)
  }

  private def parseValues(key: String, values: List[String]): Option[Masternode] = values match {
    case status :: protocol :: payee :: lastSeen :: activeSeconds :: lastPaid :: lastPaidBlock :: ip :: _ =>
      for {
        txid <- parseTxid(key)
        payee <- Address.from(payee)
        lastSeen <- Try(lastSeen.toLong).toOption
        activeSeconds <- Try(activeSeconds.toLong).toOption
      } yield Masternode(
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
