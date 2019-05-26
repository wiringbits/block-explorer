package com.xsn.explorer.data.anorm.parsers

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.{Block, BlockHeader}
import com.xsn.explorer.models.values._

object BlockParsers {

  import CommonParsers._

  val parseNextBlockhash = parseBlockhash("next_blockhash")
  val parsePreviousBlockhash = parseBlockhash("previous_blockhash")
  val parseTposContract = parseTransactionId("tpos_contract")
  val parseMerkleRoot = parseBlockhash("merkle_root")

  val parseExtractionMethod = str("extraction_method")
    .map(BlockExtractionMethod.withNameInsensitiveOption)
    .map { _.getOrElse(throw new RuntimeException("corrupted extraction_method")) }

  val parseHeight = int("height").map(Height.apply)
  val parseVersion = int("version")
  val parseMedianTime = long("median_time")
  val parseNonce = long("nonce")
  val parseBits = str("bits")
  val parseChainwork = str("chainwork")
  val parseDifficulty = get[BigDecimal]("difficulty")

  val parseBlock = (parseBlockhash() ~
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

  val parseHeader = (parseBlockhash() ~ parsePreviousBlockhash.? ~ parseMerkleRoot ~ parseHeight ~ parseTime).map {
    case blockhash ~ previousBlockhash ~ merkleRoot ~ height ~ time =>
      BlockHeader.Simple(blockhash, previousBlockhash, merkleRoot, height, time)
  }
}
