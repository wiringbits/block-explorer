package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.{Block, BlockHeader, BlockInfo}
import com.xsn.explorer.models.values._

object BlockParsers {

  import CommonParsers._

  val parseNextBlockhash = parseBlockhashBytes("next_blockhash")
  val parsePreviousBlockhash = parseBlockhashBytes("previous_blockhash")
  val parseTposContract = parseTransactionId("tpos_contract")
  val parseMerkleRoot = parseBlockhashBytes("merkle_root")

  val parseExtractionMethod = str("extraction_method")
    .map(BlockExtractionMethod.withNameInsensitiveOption)
    .map {
      _.getOrElse(throw new RuntimeException("corrupted extraction_method"))
    }

  val parseHeight = int("height").map(Height.apply)
  val parseVersion = int("version")
  val parseMedianTime = long("median_time")
  val parseNonce = long("nonce")
  val parseBits = str("bits")
  val parseChainwork = str("chainwork")
  val parseDifficulty = get[BigDecimal]("difficulty")
  val parseTransactions = int("transactions")

  val parseBlock = (parseBlockhashBytes() ~
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
    parseDifficulty ~
    parseExtractionMethod).map {

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
        difficulty ~
        extractionMethod =>
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
        version = version,
        extractionMethod = extractionMethod
      )
  }

  val parseHeader = (parseBlockhashBytes() ~
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
    parseDifficulty ~
    parseExtractionMethod).map {

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
        difficulty ~
        extractionMethod =>
      BlockHeader.Simple(
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
        version = version,
        extractionMethod = extractionMethod
      )
  }

  val parseBlockInfo = (parseBlockhashBytes() ~
    parseNextBlockhash.? ~
    parsePreviousBlockhash.? ~
    parseMerkleRoot ~
    parseHeight ~
    parseTime ~
    parseDifficulty ~
    parseTransactions ~
    parseTposContract.? ~
    parseMedianTime).map {

    case hash ~
        nextBlockhash ~
        previousBlockhash ~
        merkleRoot ~
        height ~
        time ~
        difficulty ~
        transactions ~
        tposContract ~
        medianTime =>
      BlockInfo(
        hash = hash,
        previousBlockhash = previousBlockhash,
        nextBlockhash = nextBlockhash,
        merkleRoot = merkleRoot,
        height = height,
        time = time,
        difficulty = difficulty,
        transactions = transactions,
        tposContract = tposContract,
        medianTime = medianTime
      )
  }
}
