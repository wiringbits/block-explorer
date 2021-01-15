package com.xsn.explorer.gcs

class UnsignedByte(val byte: Byte) extends AnyVal {

  override def toString: String = {
    toInt.toString
  }

  def toFixedBinaryString: String = {
    val string = toInt.toBinaryString
    val missing = List.fill(8 - string.length)(0).mkString("")
    missing + string
  }

  def toInt: Int = byte.toInt & 0xff

  def bits: List[Bit] = {
    toFixedBinaryString
      .flatMap(Bit.from)
      .toList
  }
}

object UnsignedByte {

  def parse(bits: List[Bit]): UnsignedByte = {
    require(bits.size <= 8)

    val int = bits.foldLeft(0) { case (acc, cur) =>
      (acc * 2) + cur.toInt
    }

    new UnsignedByte(int.asInstanceOf[Byte])
  }
}
