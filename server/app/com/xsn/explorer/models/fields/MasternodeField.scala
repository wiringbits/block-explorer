package com.xsn.explorer.models.fields

sealed abstract class MasternodeField(val string: String)

object MasternodeField {

  case object Status extends MasternodeField("status")
  case object IP extends MasternodeField("ip")
  case object ActiveSeconds extends MasternodeField("activeSeconds")
  case object LastSeen extends MasternodeField("lastSeen")

  def from(string: String): Option[MasternodeField] = string match {
    case Status.string        => Some(Status)
    case IP.string            => Some(IP)
    case ActiveSeconds.string => Some(ActiveSeconds)
    case LastSeen.string      => Some(LastSeen)
    case _                    => None
  }
}
