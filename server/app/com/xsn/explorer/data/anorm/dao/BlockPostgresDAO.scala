package com.xsn.explorer.data.anorm.dao

import java.sql.Connection
import javax.inject.Inject

import anorm._
import com.alexitc.playsonify.models.{Count, FieldOrdering, PaginatedQuery}
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.BlockParsers._
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Height}

class BlockPostgresDAO @Inject() (fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter) {

  def insert(block: Block)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |INSERT INTO blocks
        |  (
        |    blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |    height, version, time, median_time, nonce, bits, chainwork, difficulty
        |  )
        |VALUES
        |  (
        |    {blockhash}, {previous_blockhash}, {next_blockhash}, {tpos_contract}, {merkle_root}, {size},
        |    {height}, {version}, {time}, {median_time}, {nonce}, {bits}, {chainwork}, {difficulty}
        |  )
        |RETURNING blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty
      """.stripMargin
    ).on(
      'blockhash -> block.hash.string,
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
    ).as(parseBlock.single)
  }

  def setNextBlockhash(
      blockhash: Blockhash,
      nextBlockhash: Blockhash)(
      implicit conn: Connection): Option[Block] = {

    SQL(
      """
        |UPDATE blocks
        |SET next_blockhash = {next_blockhash}
        |WHERE blockhash = {blockhash}
        |RETURNING blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string,
      'next_blockhash -> nextBlockhash.string
    ).as(parseBlock.singleOpt).flatten
  }

  def getBy(blockhash: Blockhash)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty
        |FROM blocks
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.string
    ).as(parseBlock.singleOpt).flatten
  }

  def getBy(height: Height)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty
        |FROM blocks
        |WHERE height = {height}
      """.stripMargin
    ).on(
      "height" -> height.int
    ).as(parseBlock.singleOpt).flatten
  }

  def getBy(
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[BlockField])(
      implicit conn: Connection): List[Block] = {

    val orderBy = fieldOrderingSQLInterpreter.toOrderByClause(ordering)
    SQL(
      s"""
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty
        |FROM blocks
        |$orderBy
        |OFFSET {offset}
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'offset -> paginatedQuery.offset.int,
      'limit -> paginatedQuery.limit.int
    ).as(parseBlock.*).flatten
  }

  def count(implicit conn: Connection): Count = {
    val total = SQL(
      s"""
         |SELECT COUNT(*)
         |FROM blocks
      """.stripMargin
    ).as(SqlParser.scalar[Int].single)

    Count(total)
  }

  def delete(blockhash: Blockhash)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |DELETE FROM blocks
        |WHERE blockhash = {blockhash}
        |RETURNING blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.string
    ).as(parseBlock.singleOpt).flatten
  }

  def getLatestBlock(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty
        |FROM blocks
        |ORDER BY height DESC
        |LIMIT 1
      """.stripMargin
    ).as(parseBlock.singleOpt).flatten
  }

  def getFirstBlock(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty
        |FROM blocks
        |ORDER BY height
        |LIMIT 1
      """.stripMargin
    ).as(parseBlock.singleOpt).flatten
  }
}
