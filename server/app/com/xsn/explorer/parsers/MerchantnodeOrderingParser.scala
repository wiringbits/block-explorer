package com.xsn.explorer.parsers

import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.parsers.FieldOrderingParser
import com.xsn.explorer.models.fields.MerchantnodeField

class MerchantnodeOrderingParser extends FieldOrderingParser[MerchantnodeField] {

  override protected val defaultField = MerchantnodeField.ActiveSeconds

  override protected val defaultOrderingCondition: OrderingCondition = OrderingCondition.DescendingOrder

  override protected def parseField(unsafeField: String): Option[MerchantnodeField] = {
    MerchantnodeField.from(unsafeField)
  }
}
