package com.xsn.explorer.gcs

import com.google.common.hash.Hashing

/**
 * A Golomb-coded set, matches all items in the set with probability 1, and matches other items with probability 1/M.
 *
 * The encoding is also parameterized by P, the bit length of the remainder code.
 *
 * see https://github.com/bitcoin/bips/blob/master/bip-0158.mediawikis
 */
class GolombEncoding(p: Int, m: Int, key: SipHashKey) {
  require(p > 1 && p < 31)

  private val hasher = Hashing.sipHash24(key.k0, key.k1)

  /**
   * Encodes the given word list.
   */
  def encode(words: List[String]): GolombCodedSet = {
    val hashList = hashes(words)
    val diffList = differences(hashList)
    val encodedBits = diffList.flatMap(golombEncode)
    val encodedBytes = encodedBits
        .grouped(8)
        .map { bits => UnsignedByte.parse(bits.padTo(8, Bit.Zero)) }
        .toList

    new GolombCodedSet(
      p = p,
      m = m,
      n = words.size,
      data = encodedBytes)
  }

  /**
   * Recovers the hashes from the encoded bytes.
   *
   * This method doesn't handle corrupted inputs, which shouldn't be a problem because
   * the method is used only to verify that the filter is correct.
   *
   * @param encoded the encoded bytes, we expect them to be correct
   * @param n the number of words encoded in the bytes
   * @return the recovered list of hashes
   */
  private[gcs] def decode(encoded: List[UnsignedByte], n: Int): List[BigInt] = {
    val encodedBits = encoded.flatMap(_.bits)
    val (_, _, result) = List.fill(n)(0)
        .foldLeft((encodedBits, BigInt(0), List.empty[BigInt])) { case ((bits, acc, hashes), _) =>
          val (remaining, delta) = golombDecode(bits)
          val hash = acc + delta
          (remaining, hash, hash :: hashes)
        }

    result.reverse
  }

  /**
   * Maps the word list to a list of hashes.
   */
  private[gcs] def hashes(words: List[String]): List[BigInt] = {
    val modulus = BigInt(m) * words.length
    val f = fastReduction(_: BigInt, modulus)
    words
        .map(hash)
        .map(f)
        .sorted
  }

  private def golombEncode(x: BigInt): List[Bit] = {
    val q = (x >> p).toInt
    val r = (x & ((1 << p)-1)).toInt

    val qBits = List.fill[Bit](q)(Bit.One) :+ Bit.Zero
    val rBits = toBits(r, p)

    qBits ++ rBits
  }

  private def golombDecode(bits: List[Bit]): (List[Bit], BigInt) = {
    val q = bits.takeWhile(_ == Bit.One).size
    val rBits = bits.drop(q + 1).take(p)
    val r = toBigInt(rBits)

    val x = (q * (1L << p)) + r
    val pending = bits.drop(q + 1 + p)

    (pending, x)
  }

  private def differences(sortedHashList: List[BigInt]): List[BigInt] = {
    (BigInt(0) :: sortedHashList)
        .sliding(2)
        .map { case a :: b :: Nil => b - a }
        .toList
  }

  private def hash(string: String): BigInt = {
    val x = hasher.hashBytes(string.getBytes)
    BigInt(java.lang.Long.toUnsignedString(x.asLong()))
  }

  private def toBigInt(bits: List[Bit]): BigInt = {
    bits.foldLeft(BigInt(0)) { case (acc, cur) =>
      (acc * 2) + cur.toInt
    }
  }

  private def toBits(x: Long, size: Int): List[Bit] = {
    val bits = x
        .toBinaryString
        .flatMap(Bit.from)
        .toList

    List.fill(size - bits.size)(Bit.Zero) ++ bits
  }

  private def fastReduction(v: BigInt, modulus: BigInt): BigInt = {
    val nHi = modulus >> 32
    val nLo = modulus & 0xFFFFFFFFL

    // First, we'll spit the item we need to reduce into its higher and lower bits.
    val vhi = v >> 32
    val vlo = v & 0xFFFFFFFFL

    // Then, we distribute multiplication over each part.
    val vnphi = vhi * nHi
    val vnpmid = vhi * nLo
    val npvmid = nHi * vlo
    val vnplo = vlo * nLo

    // We calculate the carry bit.
    val carry =	((vnpmid & 0xFFFFFFFFL) + (npvmid & 0xFFFFFFFFL) + (vnplo >> 32)) >> 32

    // Last, we add the high bits, the middle bits, and the carry.
    val result = vnphi + (vnpmid >> 32) + (npvmid >> 32) + carry

    result
  }
}

object GolombEncoding {

  val DefaultP = 19
  val DefaultM = 784931

  def default(key: SipHashKey): GolombEncoding = {
    new GolombEncoding(p = DefaultP, m = DefaultM, key = key)
  }
}
