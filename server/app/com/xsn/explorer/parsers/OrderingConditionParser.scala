package com.xsn.explorer.parsers

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.{OrderingCondition, OrderingError}
import org.scalactic.{One, Or}

class OrderingConditionParser {

  def parse(unsafeOrderingCondition: String): Option[OrderingCondition] =
    unsafeOrderingCondition.toLowerCase match {

      case "asc"  => Some(OrderingCondition.AscendingOrder)
      case "desc" => Some(OrderingCondition.DescendingOrder)
      case _      => None
    }

  def parseReuslt(
      unsafeOrderingCondition: String
  ): ApplicationResult[OrderingCondition] = {

    val maybe = parse(unsafeOrderingCondition)
    Or.from(maybe, One(OrderingError.InvalidCondition))
  }
}
