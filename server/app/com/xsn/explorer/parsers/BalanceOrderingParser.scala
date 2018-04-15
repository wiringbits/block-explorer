package com.xsn.explorer.parsers

import com.xsn.explorer.models.base.OrderingCondition
import com.xsn.explorer.models.fields.BalanceField

class BalanceOrderingParser extends FieldOrderingParser[BalanceField] {

  override protected val defaultField = BalanceField.Available

  override protected val defaultOrderingCondition: OrderingCondition = OrderingCondition.DescendingOrder

  override protected def parseField(unsafeField: String): Option[BalanceField] = {
    BalanceField.from(unsafeField)
  }
}
