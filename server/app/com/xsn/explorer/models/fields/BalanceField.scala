package com.xsn.explorer.models.fields

import com.xsn.explorer.data.anorm.interpreters.ColumnNameResolver

sealed abstract class BalanceField(val string: String)

object BalanceField {

  case object Available extends BalanceField("available")
  case object Received extends BalanceField("received")
  case object Spent extends BalanceField("spent")
  case object Address extends BalanceField("address")

  def from(string: String): Option[BalanceField] = string match {
    case Available.string => Some(Available)
    case Received.string => Some(Received)
    case Spent.string => Some(Spent)
    case Address.string => Some(Address)
    case _ => None
  }

  implicit val columnNameResolver: ColumnNameResolver[BalanceField] = (field) => field match {
    case Available => s"(${Received.string} - ${Spent.string})"
    case f => f.string
  }
}
