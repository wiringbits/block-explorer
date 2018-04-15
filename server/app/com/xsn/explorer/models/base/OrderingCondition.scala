package com.xsn.explorer.models.base

sealed trait OrderingCondition

object OrderingCondition {

  case object AscendingOrder extends OrderingCondition
  case object DescendingOrder extends OrderingCondition
}
