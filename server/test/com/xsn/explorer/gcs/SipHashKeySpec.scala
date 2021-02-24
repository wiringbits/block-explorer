package com.xsn.explorer.gcs

import com.xsn.explorer.models.values.Blockhash
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec

@com.github.ghik.silencer.silent
class SipHashKeySpec extends WordSpec {

  "parsing a Btcutil-like key" should {
    "parse the right values" in {
      val bytes = List(0x4c, 0xb1, 0xab, 0x12, 0x57, 0x62, 0x1e, 0x41, 0x3b,
        0x8b, 0x0e, 0x26, 0x64, 0x8d, 0x4a, 0x15).map(_.asInstanceOf[Byte])

      val key = SipHashKey.fromBtcutil(bytes)
      key.k0 must be(4692295987881554252L)
      key.k1 must be(1534194084347808571L)
    }

    "allow to use a blockhash" in {
      val blockhash = Blockhash
        .from(
          "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32"
        )
        .get
      val expected = "SipHashKey(11873667118652460082, 11386035729178557304)"
      val key = SipHashKey.fromBtcutil(blockhash)
      key.toString must be(expected)
    }

    "calculate the same key as btcutil" in {

      /** The blockhash and the expected key were taken from btcutil tests
        * see https://github.com/btcsuite/btcutil/blob/9e5f4b9a998d263e3ce9c56664a7816001ac8000/gcs/builder/builder_test.go#L46 for the key
        * and https://github.com/btcsuite/btcutil/blob/9e5f4b9a998d263e3ce9c56664a7816001ac8000/gcs/builder/builder_test.go#L49 for the hash
        */
      val blockhash = Blockhash
        .from(
          "000000000000000000496d7ff9bd2c96154a8d64260e8b3b411e625712abb14c"
        )
        .get
      val expectedKey = SipHashKey.fromBtcutil(
        List(0x4c, 0xb1, 0xab, 0x12, 0x57, 0x62, 0x1e, 0x41, 0x3b, 0x8b, 0x0e,
          0x26, 0x64, 0x8d, 0x4a, 0x15).map(_.asInstanceOf[Byte])
      )
      val key = SipHashKey.fromBtcutil(blockhash)
      key.toString must be(expectedKey.toString)
    }
  }
}
