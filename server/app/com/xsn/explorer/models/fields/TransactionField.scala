package com.xsn.explorer.models.fields

import com.alexitc.playsonify.sql.ColumnNameResolver
import enumeratum._

sealed abstract class TransactionField(override val entryName: String) extends EnumEntry

object TransactionField extends Enum[TransactionField] {

  val values = findValues

  case object TransactionId extends TransactionField("txid")
  case object Time extends TransactionField("time")
  case object Sent extends TransactionField("sent")
  case object Received extends TransactionField("received")

  implicit val columnNameResolver: ColumnNameResolver[TransactionField] = new ColumnNameResolver[TransactionField] {

    override def getUniqueColumnName: String = TransactionId.entryName

    override def getColumnName(field: TransactionField): String = field.entryName
  }
}
