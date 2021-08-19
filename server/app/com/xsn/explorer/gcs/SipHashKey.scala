package com.xsn.explorer.gcs

import com.google.common.primitives.Longs
import com.xsn.explorer.models.values.Blockhash

/** Represents a SipHash key using a 128-bit value, compatible with Guava.
  *
  * @param k0
  *   the first half of the key
  * @param k1
  *   the second half of the key
  */
case class SipHashKey(k0: Long, k1: Long) {

  override def toString: String = {
    s"SipHashKey(${java.lang.Long.toUnsignedString(k0)}, ${java.lang.Long.toUnsignedString(k1)})"
  }
}

object SipHashKey {

  /** Parses a SipHash key in the same way that the Btcutil
    * (https://github.com/btcsuite/btcutil) library does, which uses
    * https://github.com/aead/siphash
    *
    * @param key
    *   16 bytes representing the key
    */
  def fromBtcutil(key: List[Byte]): SipHashKey = {
    require(key.size == 16, "Invalid SipHash key, it must have 16 bytes")

    /** the bytes are represented as a 128-bit encoded in little-endian while
      * guava parses the values in big-endian, that's the reason to reverse
      * them.
      */
    val k0 = Longs.fromByteArray(key.take(8).reverse.toArray)
    val k1 = Longs.fromByteArray(key.drop(8).reverse.toArray)
    SipHashKey(k0, k1)
  }

  def fromBtcutil(hash: Blockhash): SipHashKey = {
    fromBtcutil(hash.toBytesLE.take(16))
  }
}
