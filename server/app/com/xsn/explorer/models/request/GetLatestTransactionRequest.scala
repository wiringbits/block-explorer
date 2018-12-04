package com.xsn.explorer.models.request

import com.xsn.explorer.models.Address
import org.scalactic.Every
import play.api.libs.json.{Json, Reads}
import controllers.common.Codecs.everyReads

case class GetLatestTransactionRequest(addresses: Every[Address])

object GetLatestTransactionRequest {

  implicit val reads: Reads[GetLatestTransactionRequest] = Json.reads[GetLatestTransactionRequest]
}
