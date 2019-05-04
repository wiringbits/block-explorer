package com.xsn.explorer.models.values

class HexString private (val string: String) extends AnyVal {

  override def toString: String = string
}

object HexString {

  val Empty = new HexString("")

  private val RegEx = "^([a-fA-F0-9][a-fA-F0-9])*$"

  def from(string: String): Option[HexString] = {
    if (string.isEmpty) {
      Some(Empty)
    } else if (string.matches(RegEx)) {
      Option(new HexString(string))
    } else {
      None
    }
  }
}
