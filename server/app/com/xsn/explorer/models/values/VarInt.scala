package com.xsn.explorer.models.values

import play.api.libs.json._

object VarInt {

  def from(n: Int): HexString = {
    val hexLittleEndian = padHexString(n.toHexString).grouped(2).toList.reverse.mkString("")
    val prefix = getPrefix(n)

    HexString.from(prefix + hexLittleEndian).get
  }

  private def getPrefix(n: Int) = {
    if (n <= Integer.parseInt("FC", 16)) {
      ""
    } else if (n <= Integer.parseInt("FFFF", 16)) {
      "fd"
    } else {
      "fe"
    }
  }

  private def padHexString(hex: String) = {
    if (hex.length % 2 == 0) {
      hex
    } else {
      "0" + hex
    }
  }
}
