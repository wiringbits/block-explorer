package com.xsn.explorer.parsers

import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.parsers.FieldOrderingParser
import com.xsn.explorer.models.fields.BalanceField

class BalanceOrderingParser extends FieldOrderingParser[BalanceField] {

  override protected val defaultField = BalanceField.Available

  override protected val defaultOrderingCondition: OrderingCondition =
    OrderingCondition.DescendingOrder

  override protected def parseField(
      unsafeField: String
  ): Option[BalanceField] = {
    BalanceField.withNameOption(unsafeField)
  }
}
