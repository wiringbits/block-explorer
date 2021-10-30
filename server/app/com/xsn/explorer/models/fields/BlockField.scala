package com.xsn.explorer.models.fields

import com.alexitc.playsonify.sql.ColumnNameResolver
import enumeratum.{Enum, EnumEntry}

sealed abstract class BlockField(override val entryName: String) extends EnumEntry

object BlockField extends Enum[BlockField] {

  val values = findValues

  case object Height extends BlockField("height")
  case object Time extends BlockField("time")

  implicit val columnNameResolver: ColumnNameResolver[BlockField] =
    new ColumnNameResolver[BlockField] {

      override def getUniqueColumnName: String = Height.entryName

      override def getColumnName(field: BlockField): String = field.entryName
    }
}
