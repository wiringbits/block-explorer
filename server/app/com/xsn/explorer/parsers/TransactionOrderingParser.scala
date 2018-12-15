package com.xsn.explorer.parsers

import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.parsers.FieldOrderingParser
import com.xsn.explorer.models.fields.TransactionField

class TransactionOrderingParser extends FieldOrderingParser[TransactionField] {

  override protected val defaultField = TransactionField.Time

  override protected val defaultOrderingCondition: OrderingCondition = OrderingCondition.DescendingOrder

  override protected def parseField(unsafeField: String): Option[TransactionField] = {
    TransactionField.withNameOption(unsafeField)
  }
}
