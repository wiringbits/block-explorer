package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.Block

object BlockParsers {

  val parseHash = str("hash").map(Blockhash.from)
  val parseNextBlockhash = str("next_blockhash").map(Blockhash.from)
  val parsePreviousBlockhash = str("previous_blockhash").map(Blockhash.from)
  val parseTposContract = str("tpos_contract").map(TransactionId.from)
  val parseMerkleRoot = str("merkle_root").map(Blockhash.from)
  val parseSize = int("size").map(Size.apply)
  val parseHeight = int("height").map(Height.apply)
  val parseVersion = int("version")
  val parseTime = long("time")
  val parseMedianTime = long("median_time")
  val parseNonce = int("nonce")
  val parseBits = str("bits")
  val parseChainwork = str("chainwork")
  val parseDifficulty = get[BigDecimal]("difficulty")

  val parseBlock = (
      parseHash ~
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

    case hashMaybe ~
        nextBlockhash ~
        previousBlockhash ~
        tposContract ~
        merkleRootMaybe ~
        size ~
        height ~
        version ~
        time ~
        medianTime ~
        nonce ~
        bits ~
        chainwork ~
        difficulty =>

      for {
        hash <- hashMaybe
        merkleRoot <- merkleRootMaybe
      } yield Block(
        hash = hash,
        previousBlockhash = previousBlockhash.flatten,
        nextBlockhash = nextBlockhash.flatten,
        tposContract = tposContract.flatten,
        merkleRoot = merkleRoot,
        size = size,
        height = height,
        time = time,
        medianTime = medianTime,
        nonce = nonce,
        bits = bits,
        chainwork = chainwork,
        difficulty = difficulty,
        version = version,
        transactions = List.empty,
        confirmations = Confirmations(0)
      )
  }
}
