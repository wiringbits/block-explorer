package com.xsn.explorer.models.values

import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class CompactSizeIntSpec extends WordSpec with MustMatchers with OptionValues {
  "CompactSizeUInt" should {
    "serialize a VarInt with size 1 correctly" in {
      val varInt = CompactSizeInt(139, 1)
      varInt.hex.string must be("8b")
    }

    "serialize a VarInt with that is the number zero correctly" in {
      val varInt = CompactSizeInt(0, 1)
      varInt.hex.string must be("00")
    }

    "serialize a compact size uint representing 255" in {
      val compactSizeUInt = CompactSizeInt(255)
      compactSizeUInt must be(CompactSizeInt(255, 3))
      compactSizeUInt.hex.string must be("fdff00")
    }

    "serialize a compact size uint representing 515" in {
      val compactSizeUInt = CompactSizeInt(515)
      compactSizeUInt must be(CompactSizeInt(515, 3))
      compactSizeUInt.hex.string must be("fd0302")
    }

    "serialize a compact size uint representing 500000" in {
      val compactSizeUInt = CompactSizeInt(500000)
      compactSizeUInt must be(CompactSizeInt(500000, 5))
      compactSizeUInt.hex.string must be("fe20a10700")
    }
  }
}
