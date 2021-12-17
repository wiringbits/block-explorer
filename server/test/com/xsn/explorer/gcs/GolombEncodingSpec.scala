package com.xsn.explorer.gcs

import com.google.common.io.BaseEncoding
import com.xsn.explorer.helpers.BlockLoader
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

@com.github.ghik.silencer.silent
class GolombEncodingSpec extends AnyWordSpec {

  val words = Set(
    "Alex",
    "Bob",
    "Charlie",
    "Dick",
    "Ed",
    "Frank",
    "George",
    "Harry",
    "Ilya",
    "John",
    "Kevin",
    "Larry",
    "Michael",
    "Nate",
    "Owen",
    "Paul",
    "Quentin"
  )

  "the encoding" should {
    val keyBytes = List(0x4c, 0xb1, 0xab, 0x12, 0x57, 0x62, 0x1e, 0x41, 0x3b, 0x8b, 0x0e, 0x26, 0x64, 0x8d, 0x4a, 0x15)
      .map(_.asInstanceOf[Byte])

    val key = SipHashKey.fromBtcutil(keyBytes)
    val golomb = GolombEncoding.default(key)
    val encoded = golomb.encode(words)

    "decode the same hashes" in {
      val hashes = golomb.hashes(words.map(_.getBytes))
      val bytes = BaseEncoding
        .base16()
        .decode(encoded.hex.string.toUpperCase)
        .toList
        .map(new UnsignedByte(_))

      val decoded = golomb.decode(bytes, words.size)

      decoded mustEqual hashes
    }

    "return the encoded hex from the btcutil gcs" in {

      /** The hex was generated from this go code:
        * {{{
        * package main
        *
        * import (
        *   "encoding/hex"
        *   "fmt"
        *   "github.com/btcsuite/btcutil/gcs/builder"
        * )
        *
        * func main() {
        *   contents := [][]byte{
        *       []byte("Alex"),
        *       []byte("Bob"),
        *       []byte("Charlie"),
        *       []byte("Dick"),
        *       []byte("Ed"),
        *       []byte("Frank"),
        *       []byte("George"),
        *       []byte("Harry"),
        *       []byte("Ilya"),
        *       []byte("John"),
        *       []byte("Kevin"),
        *       []byte("Larry"),
        *       []byte("Michael"),
        *       []byte("Nate"),
        *       []byte("Owen"),
        *       []byte("Paul"),
        *       []byte("Quentin"),
        *   }
        *   testKey := [16]byte{0x4c, 0xb1, 0xab, 0x12, 0x57, 0x62, 0x1e, 0x41,
        *       0x3b, 0x8b, 0x0e, 0x26, 0x64, 0x8d, 0x4a, 0x15}
        *
        *   b := builder.WithRandomKey().SetKey(testKey);
        *   f, err := b.AddEntries(contents).Build();
        *   if err != nil {
        *       fmt.Println("Error", err)
        *   }
        *   rawBytes, _ := f.Bytes()
        *   encoded := hex.EncodeToString(rawBytes);
        *   fmt.Println("Filter: %X\n", len(encoded), encoded)
        * }
        * }}}
        */
      val expected =
        "056ff79e6c2994ba5d91402f327f807097c5c571f8d212511a8237f005331346102b41967f35ef488406c38a88"
      encoded.hex.string must be(expected)
    }

    "calculate expected filters for BIP-158 test cases" should {
      val testCases = List(
        // Test cases https://github.com/bitcoin/bips/blob/master/bip-0158/testnet-19.json
        (
          "000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943",
          "019dfca8"
        ),
        (
          "000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820",
          "0174a170"
        ),
        (
          "000000008b896e272758da5297bcd98fdc6d97c9b765ecec401e286dc1fdbe10",
          "016cf7a0"
        ),
        (
          "0000000038c44c703bae0f98cdd6bf30922326340a5996cc692aaae8bacf47ad",
          "013c3710"
        ),
        (
          "0000000018b07dca1b28b4b5a119f6d6e71698ce1ed96f143f54179ce177a19c",
          "0afbc2920af1b027f31f87b592276eb4c32094bb4d3697021b4c6380"
        ),
        (
          "00000000fd3ceb2404ff07a785c7fdcc76619edc8ed61bd25134eaa22084366a",
          "0db414c859a07e8205876354a210a75042d0463404913d61a8e068e58a3ae2aa080026"
        ),
        (
          "000000000000015d6077a411a8f5cc95caf775ccf11c54e27df75ce58d187313",
          "09027acea61b6cc3fb33f5d52f7d088a6b2f75d234e89ca800"
        ),
        (
          "0000000000000c00901f2049055e2a437c819d79a3d54fd63e6af796cd7b8a79",
          "010c0b40"
        ),
        (
          "000000006f27ddfe1dd680044a34548f41bed47eba9e6f0b310da21423bc5f33",
          "0385acb4f0fe889ef0"
        ),
        (
          "0000000000000027b2b3b3381f114f674f481544ff2be37ae3788d7e078383b1",
          "00"
        )
      )

      testCases.foreach { case (blockhash, expectedFilter) =>
        val block = BlockLoader.getFullRPCWithValues(blockhash, "btc")
        s"succeed on block ${block.height}" in {
          val blockFilter = GolombEncoding.encode(block)

          blockFilter.getHexString.string must be(expectedFilter)
        }
      }
    }
  }
}
