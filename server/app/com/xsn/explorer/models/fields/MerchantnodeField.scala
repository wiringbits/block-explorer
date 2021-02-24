package com.xsn.explorer.models.fields

sealed abstract class MerchantnodeField(val string: String)

object MerchantnodeField {

  case object Status extends MerchantnodeField("status")
  case object IP extends MerchantnodeField("ip")
  case object ActiveSeconds extends MerchantnodeField("activeSeconds")
  case object LastSeen extends MerchantnodeField("lastSeen")

  def from(string: String): Option[MerchantnodeField] = string match {
    case Status.string        => Some(Status)
    case IP.string            => Some(IP)
    case ActiveSeconds.string => Some(ActiveSeconds)
    case LastSeen.string      => Some(LastSeen)
    case _                    => None
  }
}
