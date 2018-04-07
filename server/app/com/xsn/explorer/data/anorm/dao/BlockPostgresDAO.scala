package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.BlockParsers._
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block

class BlockPostgresDAO {

  def create(block: Block)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |INSERT INTO blocks
        |  (
        |    hash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |    height, version, time, median_time, nonce, bits, chainwork, difficulty
        |  )
        |VALUES
        |  (
        |    {hash}, {previous_blockhash}, {next_blockhash}, {tpos_contract}, {merkle_root}, {size},
        |    {height}, {version}, {time}, {median_time}, {nonce}, {bits}, {chainwork}, {difficulty}
        |  )
        |ON CONFLICT (hash)
        |DO UPDATE
        |  SET previous_blockhash = EXCLUDED.previous_blockhash,
        |      next_blockhash = EXCLUDED.next_blockhash,
        |      tpos_contract = EXCLUDED.tpos_contract,
        |      merkle_root = EXCLUDED.merkle_root,
        |      size = EXCLUDED.size,
        |      height = EXCLUDED.height,
        |      version = EXCLUDED.version,
        |      time = EXCLUDED.time,
        |      median_time = EXCLUDED.median_time,
        |      nonce = EXCLUDED.nonce,
        |      bits = EXCLUDED.bits,
        |      chainwork = EXCLUDED.chainwork,
        |      difficulty = EXCLUDED.difficulty
        |RETURNING hash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty
      """.stripMargin
    ).on(
      'hash -> block.hash.string,
      'previous_blockhash -> block.previousBlockhash.map(_.string),
      'next_blockhash -> block.nextBlockhash.map(_.string),
      'tpos_contract -> block.tposContract.map(_.string),
      'merkle_root -> block.merkleRoot.string,
      'size -> block.size.int,
      'height -> block.height.int,
      'version -> block.version,
      'time -> block.time,
      'median_time -> block.medianTime,
      'nonce -> block.nonce,
      'bits -> block.bits,
      'chainwork -> block.chainwork,
      'difficulty -> block.difficulty
    ).as(parseBlock.singleOpt).flatten
  }

  def getBy(blockhash: Blockhash)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |SELECT hash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty
        |FROM blocks
        |WHERE hash = {hash}
      """.stripMargin
    ).on(
      "hash" -> blockhash.string
    ).as(parseBlock.singleOpt).flatten
  }
}
