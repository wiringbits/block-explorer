package com.xsn.explorer.models.values

/**
 * Compact sized integer, a Bitcoin-native data structure
 *
 * @see https://bitcoin.org/en/developer-reference#compactsize-unsigned-integers
 */
sealed abstract class CompactSizeInt {

  /** The number parsed from VarInt. */
  def num: Long

  /** The size in bytes of num */
  def size: Long

  def hex = size match {
    case 1 => flipEndianness(f"$num%016x".slice(14, 16))
    case 3 => "fd" + flipEndianness(f"$num%016x".slice(12, 16))
    case 5 => "fe" + flipEndianness(f"$num%016x".slice(8, 16))
    case _ => "ff" + flipEndianness(f"$num%016x")
  }

  private def flipEndianness(hex: String) = {
    hex.grouped(2).toList.reverse.mkString
  }
}

object CompactSizeInt {
  private case class CompactSizeIntImpl(override val num: Long, override val size: Long) extends CompactSizeInt

  val zero: CompactSizeInt = CompactSizeInt(0L)

  def apply(num: Long, size: Int): CompactSizeInt = {
    CompactSizeIntImpl(num, size)
  }

  def apply(num: Long): CompactSizeInt = {
    val size = calcSizeForNum(num)
    CompactSizeInt(num, size)
  }

  private def calcSizeForNum(num: Long): Int = {
    if (num <= 252) 1
    // can be represented with two bytes
    else if (num <= 65535) 3
    //can be represented with 4 bytes
    else if (num <= Integer.MAX_VALUE) 5
    else 9
  }
}
