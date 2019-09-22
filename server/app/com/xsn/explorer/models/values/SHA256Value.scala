package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedString
import javax.xml.bind.DatatypeConverter

trait SHA256Value extends Any with WrappedString {

  def toBytesBE: List[Byte] = {
    string
      .grouped(2)
      .toList
      .map { hex =>
        Integer.parseInt(hex, 16).asInstanceOf[Byte]
      }
  }

  def toBytesLE: List[Byte] = {
    toBytesBE.reverse
  }
}

object SHA256Value {

  private class Default(val string: String) extends SHA256Value

  val Length = 64

  private val pattern = "^[a-f0-9]{64}$".r.pattern

  def from(string: String): Option[SHA256Value] = {
    val lowercaseString = string.toLowerCase

    if (pattern.matcher(lowercaseString).matches()) {
      Some(new Default(lowercaseString))
    } else {
      None
    }
  }

  def fromBytesBE(bytes: Array[Byte]): Option[SHA256Value] = {
    val string = DatatypeConverter.printHexBinary(bytes)
    from(string)
  }
}
