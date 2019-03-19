package com.xsn.explorer.gcs

import com.google.common.io.BaseEncoding
import org.scalatest.{MustMatchers, WordSpec}

class GolombEncodingSpec extends WordSpec with MustMatchers {

  val words = List(
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
    val keyBytes = List(
      0x4c, 0xb1, 0xab, 0x12, 0x57, 0x62, 0x1e, 0x41,
      0x3b, 0x8b, 0x0e, 0x26, 0x64, 0x8d, 0x4a, 0x15).map(_.asInstanceOf[Byte])

    val key = SipHashKey.fromBtcutil(keyBytes)
    val golomb = GolombEncoding.default(key)
    val encoded = golomb.encode(words)

    "decode the same hashes" in {
      val hashes = golomb.hashes(words)
      val bytes = BaseEncoding
          .base16()
          .decode(encoded.hex.string.toUpperCase)
          .toList
          .map(new UnsignedByte(_))

      val decoded = golomb.decode(bytes, words.size)

      decoded mustEqual hashes
    }

    "return the encoded hex from the btcutil gcs" in {
      /**
       * The hex was generated from this go code:
{{{
package main

import (
   "encoding/hex"
   "fmt"
   "github.com/btcsuite/btcutil/gcs/builder"
)

func main() {
   contents := [][]byte{
       []byte("Alex"),
       []byte("Bob"),
       []byte("Charlie"),
       []byte("Dick"),
       []byte("Ed"),
       []byte("Frank"),
       []byte("George"),
       []byte("Harry"),
       []byte("Ilya"),
       []byte("John"),
       []byte("Kevin"),
       []byte("Larry"),
       []byte("Michael"),
       []byte("Nate"),
       []byte("Owen"),
       []byte("Paul"),
       []byte("Quentin"),
   }
   testKey := [16]byte{0x4c, 0xb1, 0xab, 0x12, 0x57, 0x62, 0x1e, 0x41,
       0x3b, 0x8b, 0x0e, 0x26, 0x64, 0x8d, 0x4a, 0x15}

   b := builder.WithRandomKey().SetKey(testKey);
   f, err := b.AddEntries(contents).Build();
   if err != nil {
       fmt.Println("Error", err)
   }
   rawBytes, _ := f.Bytes()
   encoded := hex.EncodeToString(rawBytes);
   fmt.Println("Filter: %X\n", len(encoded), encoded)
}
}}}
       */
      val expected = "056ff79e6c2994ba5d91402f327f807097c5c571f8d212511a8237f005331346102b41967f35ef488406c38a88"
      encoded.hex.string must be(expected)
    }
  }
}
