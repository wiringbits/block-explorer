package com.xsn.explorer.gcs

sealed trait Bit extends Product with Serializable {

  def toInt: Int = this match {
    case Bit.Zero => 0
    case Bit.One => 1
  }

  override def toString: String = toInt.toString
}

object Bit {
  final case object Zero extends Bit
  final case object One extends Bit

  def from(char: Char): Option[Bit] = char match {
    case '0' => Option(Bit.Zero)
    case '1' => Option(Bit.One)
    case _ => None
  }
}
