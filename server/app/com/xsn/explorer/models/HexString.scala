package com.xsn.explorer.models

class HexString private (val string: String) extends AnyVal {

  override def toString: String = string
}

object HexString {

  private val RegEx = "^[A-Fa-f0-9]+$"

  def from(string: String): Option[HexString] = {
    if (string.length % 2 == 0 && string.matches(RegEx)) {
      Option(new HexString(string))
    } else {
      None
    }
  }
}
