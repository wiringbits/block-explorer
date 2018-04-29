package com.xsn.explorer.util

object Extensions {

  private val SatoshiScale = 100000000L

  implicit class BigDecimalExt(val inner: BigDecimal) extends AnyVal {
    def fromSatoshis: BigDecimal = {
      inner / SatoshiScale
    }
  }

  implicit class ListOptionExt[+A](val inner: List[Option[A]]) extends AnyVal {
    def everything: Option[List[A]] = {
      if (inner.forall(_.isDefined)) {
        Some(inner.flatten)
      } else {
        None
      }
    }
  }
}
