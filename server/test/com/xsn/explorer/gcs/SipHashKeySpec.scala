package com.xsn.explorer.gcs

import org.scalatest.MustMatchers._
import org.scalatest.WordSpec

class SipHashKeySpec extends WordSpec {

  "parsing a Btcutil-like key" should {
    "parse the right values" in {
      val bytes = List(
        0x4c, 0xb1, 0xab, 0x12, 0x57, 0x62, 0x1e, 0x41,
        0x3b, 0x8b, 0x0e, 0x26, 0x64, 0x8d, 0x4a, 0x15).map(_.asInstanceOf[Byte])

      val key = SipHashKey.fromBtcutil(bytes)
      key.k0 must be(4692295987881554252L)
      key.k1 must be(1534194084347808571l)
    }
  }
}
