package com.xsn.explorer.gcs

import com.xsn.explorer.models.values.HexString

class GolombCodedSet(
    val p: Int,
    val m: Int,
    val n: Int,
    val data: List[UnsignedByte]) {

  def hex: HexString = {
    val string = data.map(_.byte).map("%02x".format(_)).mkString("")
    HexString.from(string) match {
      case Some(value) => value
      case None => throw new RuntimeException("Unexpected error, unable to create hex value")
    }
  }
}
