package com.xsn.explorer.models.values

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

@com.github.ghik.silencer.silent
class HexStringSpec extends AnyWordSpec with Matchers with OptionValues {

  "from" should {
    "accept a valid hex" in {
      val string =
        "0100000001d036c70b1df769fa3205f8ff4e361af84073aa14c89de80488048b6ae4904ce9010000006a47304402201f1f9aef5d60f6e84714dfb98ca87ca8a146a2e04a3811d8f0aa770d8ac1c906022054e27a26f806a5d0c0e08332be186a96ee1ac951b8d1e6e3b10072d51eb6dd300121026648fd298f1cc06c474db0864720a9774efbe789dd67b2a46086f9754e4cc3f2ffffffff030000000000000000000e77b9932d0000001976a91436e0c51c93a357e23621bb993d28e5c18f95bb5588ac00d2496b000000001976a9149d1fef13c02f2f23cf9a09ba11987e90Dbf5910d88ac00000000"
      val result = HexString.from(string)
      result.value.string mustEqual string
    }

    "accept a valid hex with mixed case" in {
      val string =
        "0100000001d036c70b1df769fA3205f8fF4e361af84073aa14c89de80488048b6aE4904ce9010000006a47304402201f1f9aef5d60f6e84714dfb98ca87ca8a146a2e04a3811d8f0aa770d8ac1c906022054e27a26f806a5d0c0e08332be186a96ee1ac951b8d1e6e3b10072d51eb6dd300121026648fd298f1cc06c474db0864720a9774efbe789dd67b2a46086f9754e4cc3f2ffffffff030000000000000000000e77b9932d0000001976a91436e0c51c93a357e23621bb993d28e5c18f95bb5588ac00d2496b000000001976a9149d1fef13c02f2f23cf9a09ba11987e90Dbf5910d88ac00000000"
      val result = HexString.from(string)
      result.value.string mustEqual string
    }

    "accept a string with all hex characters" in {
      val string = "abcdef0123456789ABCDEF"
      val result = HexString.from(string)
      result.value.string mustEqual string
    }

    "accept a two characters string" in {
      val string = "0f"
      val result = HexString.from(string)
      result.value.string mustEqual string
    }

    "accept an empty string" in {
      val string = ""
      val result = HexString.from(string)
      result.nonEmpty mustEqual true
    }

    "reject a single character" in {
      val string = "a"
      val result = HexString.from(string)
      result.isEmpty mustEqual true
    }

    "reject spaces" in {
      val string = "aa "
      val result = HexString.from(string)
      result.isEmpty mustEqual true
    }

    "reject non-hex characters" in {
      val string = "abcdefgh"
      val result = HexString.from(string)
      result.isEmpty mustEqual true
    }
  }
}
