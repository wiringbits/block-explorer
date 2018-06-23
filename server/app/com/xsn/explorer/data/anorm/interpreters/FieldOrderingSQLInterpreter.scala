package com.xsn.explorer.data.anorm.interpreters

import com.alexitc.playsonify.models.{FieldOrdering, OrderingCondition}

class FieldOrderingSQLInterpreter {

  def toOrderByClause[A](fieldOrdering: FieldOrdering[A])(implicit columnNameResolver: ColumnNameResolver[A]) = {
    val field = columnNameResolver.getColumnName(fieldOrdering.field)
    val condition = getCondition(fieldOrdering.orderingCondition)
    val uniqueField = columnNameResolver.getUniqueColumnName

    if (field == uniqueField) {
      s"ORDER BY $field $condition"
    } else {
      s"ORDER BY $field $condition, $uniqueField"
    }
  }

  private def getCondition(ordering: OrderingCondition) = ordering match {
    case OrderingCondition.AscendingOrder => "ASC"
    case OrderingCondition.DescendingOrder => "DESC"
  }
}
