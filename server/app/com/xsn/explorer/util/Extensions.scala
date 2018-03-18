package com.xsn.explorer.util

object Extensions {

  private val SatoshiScale = 100000000L

  implicit class BigDecimalExt(val inner: BigDecimal) extends AnyVal {
    def fromSatoshis: BigDecimal = {
      inner / SatoshiScale
    }
  }
}
