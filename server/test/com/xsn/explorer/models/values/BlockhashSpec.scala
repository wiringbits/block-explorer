package com.xsn.explorer.models.values

import org.scalatest.WordSpec
import org.scalatest.MustMatchers._

@com.github.ghik.silencer.silent
class BlockhashSpec extends WordSpec {
  val blockhash = Blockhash
    .from("000000000000000000496d7ff9bd2c96154a8d64260e8b3b411e625712abb14c")
    .get

  "Getting the blockhash bytes in little endian" should {
    "return the correct bytes" in {
      val expectedBytes: List[Byte] =
        List(76, -79, -85, 18, 87, 98, 30, 65, 59, -117, 14, 38, 100, -115, 74, 21, -106, 44, -67, -7, 127, 109, 73, 0,
          0, 0, 0, 0, 0, 0, 0, 0)

      blockhash.toBytesLE must be(expectedBytes)
    }
  }
}
