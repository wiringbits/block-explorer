package com.xsn.explorer.models

import play.api.libs.json.{Json, Writes}

case class NodeStatistics(
    masternodes: Int,
    enabledMasternodes: Int,
    masternodesProtocols: Map[String, Int],
    tposnodes: Int,
    enabledTposnodes: Int,
    tposnodesProtocols: Map[String, Int]
)

object NodeStatistics {

  implicit val writes: Writes[NodeStatistics] = Json.writes[NodeStatistics]
}
