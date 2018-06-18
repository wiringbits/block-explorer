package com.xsn.explorer.models.request

import play.api.libs.json.{Json, Reads}

case class SendRawTransactionRequest(hex: String)

object SendRawTransactionRequest {

  implicit val reads: Reads[SendRawTransactionRequest] = Json.reads[SendRawTransactionRequest]
}
