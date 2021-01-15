package com.xsn.explorer.models.fields

import com.alexitc.playsonify.sql.ColumnNameResolver
import enumeratum._

sealed abstract class BalanceField(override val entryName: String)
    extends EnumEntry

object BalanceField extends Enum[BalanceField] {

  val values = findValues

  case object Available extends BalanceField("available")
  case object Received extends BalanceField("received")
  case object Spent extends BalanceField("spent")
  case object Address extends BalanceField("address")

  implicit val columnNameResolver: ColumnNameResolver[BalanceField] =
    new ColumnNameResolver[BalanceField] {

      override def getUniqueColumnName: String = Address.entryName

      override def getColumnName(field: BalanceField): String = field match {
        case Available => s"(${Received.entryName} - ${Spent.entryName})"
        case f         => f.entryName
      }
    }
}
