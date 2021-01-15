package com.xsn.explorer.models.request

import com.xsn.explorer.models.values.Address
import play.api.libs.json._

case class TposContractsEncodeRequest(
    tposAddress: Address,
    merchantAddress: Address,
    commission: Int,
    signature: String
)

object TposContractsEncodeRequest {
  implicit val reads: Reads[TposContractsEncodeRequest] =
    Json.reads[TposContractsEncodeRequest]
}
