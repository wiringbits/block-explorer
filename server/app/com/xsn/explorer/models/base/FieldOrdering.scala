package com.xsn.explorer.models.base

case class FieldOrdering[+A](field: A, orderingCondition: OrderingCondition)
