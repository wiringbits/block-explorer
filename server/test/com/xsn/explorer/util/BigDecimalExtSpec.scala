package com.xsn.explorer.util

import org.scalatest.{MustMatchers, WordSpec}

@com.github.ghik.silencer.silent
class BigDecimalExtSpec extends WordSpec with MustMatchers {

  import Extensions.BigDecimalExt

  "fromSatoshis" should {
    "work" in {
      val input = BigDecimal(40409891838L)
      val result = input.fromSatoshis
      result mustEqual BigDecimal(404.09891838)
    }
  }

  "toSatoshis" should {
    "work" in {
      val input = BigDecimal(404.09891838)
      val result = input.toSatoshis
      result mustEqual BigDecimal(40409891838L)
    }
  }
}
