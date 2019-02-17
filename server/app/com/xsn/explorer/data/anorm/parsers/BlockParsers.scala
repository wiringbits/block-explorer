package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Block

object BlockParsers {

  import CommonParsers._

  val parseNextBlockhash = str("next_blockhash")
      .map(Blockhash.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted next_blockhash")) }

  val parsePreviousBlockhash = str("previous_blockhash")
      .map(Blockhash.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted previous_blockhash")) }

  val parseTposContract = str("tpos_contract")
      .map(TransactionId.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted tpos_contract")) }

  val parseMerkleRoot = str("merkle_root")
      .map(Blockhash.from)
      .map { _.getOrElse(throw new RuntimeException("corrupted merkle_root")) }

  val parseSize = int("size").map(Size.apply)
  val parseHeight = int("height").map(Height.apply)
  val parseVersion = int("version")
  val parseMedianTime = long("median_time")
  val parseNonce = long("nonce")
  val parseBits = str("bits")
  val parseChainwork = str("chainwork")
  val parseDifficulty = get[BigDecimal]("difficulty")

  val parseBlock = (
      parseBlockhash ~
          parseNextBlockhash.? ~
          parsePreviousBlockhash.? ~
          parseTposContract.? ~
          parseMerkleRoot ~
          parseSize ~
          parseHeight ~
          parseVersion ~
          parseTime ~
          parseMedianTime ~
          parseNonce ~
          parseBits ~
          parseChainwork ~
          parseDifficulty).map {

    case hash ~
        nextBlockhash ~
        previousBlockhash ~
        tposContract ~
        merkleRoot ~
        size ~
        height ~
        version ~
        time ~
        medianTime ~
        nonce ~
        bits ~
        chainwork ~
        difficulty =>

      Block(
        hash = hash,
        previousBlockhash = previousBlockhash,
        nextBlockhash = nextBlockhash,
        tposContract = tposContract,
        merkleRoot = merkleRoot,
        size = size,
        height = height,
        time = time,
        medianTime = medianTime,
        nonce = nonce,
        bits = bits,
        chainwork = chainwork,
        difficulty = difficulty,
        version = version
      )
  }
}
