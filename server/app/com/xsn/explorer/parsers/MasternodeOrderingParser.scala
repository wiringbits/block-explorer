package com.xsn.explorer.parsers

import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.parsers.FieldOrderingParser
import com.xsn.explorer.models.fields.MasternodeField

class MasternodeOrderingParser extends FieldOrderingParser[MasternodeField] {

  override protected val defaultField = MasternodeField.ActiveSeconds

  override protected val defaultOrderingCondition: OrderingCondition =
    OrderingCondition.DescendingOrder

  override protected def parseField(
      unsafeField: String
  ): Option[MasternodeField] = {
    MasternodeField.from(unsafeField)
  }
}
