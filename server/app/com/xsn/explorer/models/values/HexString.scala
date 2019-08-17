package com.xsn.explorer.models.values

import javax.xml.bind.DatatypeConverter

class HexString private (val string: String) extends AnyVal {

  override def toString: String = string

  def toBytes: Array[Byte] = string.grouped(2).map(Integer.parseInt(_, 16).asInstanceOf[Byte]).toArray

  def concat(hex: HexString): HexString = new HexString(this.string + hex.string)

  def drop(count: Int) = new HexString(string.drop(count))

  def length = this.string.length
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

  def fromBytesBE(bytes: Array[Byte]): HexString = {
    if (bytes.isEmpty) {
      Empty
    } else {
      val string = DatatypeConverter.printHexBinary(bytes).toLowerCase()
      new HexString(string)
    }
  }
}
