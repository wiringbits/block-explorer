package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Count, Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.BlockParsers._
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.persisted.{Block, BlockHeader}
import com.xsn.explorer.models.values.{Blockhash, Height}
import javax.inject.Inject

class BlockPostgresDAO @Inject() (fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter) {

  def insert(block: Block)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |INSERT INTO blocks
        |  (
        |    blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |    height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |  )
        |VALUES
        |  (
        |    {blockhash}, {previous_blockhash}, {next_blockhash}, {tpos_contract}, {merkle_root}, {size},
        |    {height}, {version}, {time}, {median_time}, {nonce}, {bits}, {chainwork}, {difficulty}, {extraction_method}::BLOCK_EXTRACTION_METHOD_TYPE
        |  )
        |RETURNING blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
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
      'difficulty -> block.difficulty,
      'extraction_method -> block.extractionMethod.entryName
    ).as(parseBlock.singleOpt)
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
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string,
      'next_blockhash -> nextBlockhash.string
    ).as(parseBlock.singleOpt)
  }

  def getBy(blockhash: Blockhash)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |FROM blocks
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.string
    ).as(parseBlock.singleOpt)
  }

  def getBy(height: Height)(implicit conn: Connection): Option[Block] = {
    SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |FROM blocks
        |WHERE height = {height}
      """.stripMargin
    ).on(
      "height" -> height.int
    ).as(parseBlock.singleOpt)
  }

  def getBy(
      paginatedQuery: PaginatedQuery,
      ordering: FieldOrdering[BlockField])(
      implicit conn: Connection): List[Block] = {

    val orderBy = fieldOrderingSQLInterpreter.toOrderByClause(ordering)
    SQL(
      s"""
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |FROM blocks
        |$orderBy
        |OFFSET {offset}
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'offset -> paginatedQuery.offset.int,
      'limit -> paginatedQuery.limit.int
    ).as(parseBlock.*)
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
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.string
    ).as(parseBlock.singleOpt)
  }

  def getLatestBlock(implicit conn: Connection): Option[Block] = {
    val query = PaginatedQuery(Offset(0), Limit(1))
    val ordering = FieldOrdering(BlockField.Height, OrderingCondition.DescendingOrder)
    getBy(query, ordering).headOption
  }

  def getFirstBlock(implicit conn: Connection): Option[Block] = {
    val query = PaginatedQuery(Offset(0), Limit(1))
    val ordering = FieldOrdering(BlockField.Height, OrderingCondition.AscendingOrder)
    getBy(query, ordering).headOption
  }

  def getHeaders(limit: Limit, orderingCondition: OrderingCondition)(implicit conn: Connection): List[BlockHeader] = {
    val order = toSQL(orderingCondition)

    SQL(
      s"""
        |SELECT blockhash, previous_blockhash, merkle_root, height, time
        |FROM blocks
        |ORDER BY height $order
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'limit -> limit.int
    ).as(parseHeader.*)
  }

  def getHeaders(lastSeenHash: Blockhash, limit: Limit, orderingCondition: OrderingCondition)(implicit conn: Connection): List[BlockHeader] = {
    val order = toSQL(orderingCondition)
    val comparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder => ">"
    }

    SQL(
      s"""
        |WITH CTE AS (
        |  SELECT height as lastSeenHeight
        |  FROM blocks
        |  WHERE blockhash = {lastSeenHash}
        |)
        |SELECT b.blockhash, b.previous_blockhash, b.merkle_root, b.height, b.time
        |FROM CTE CROSS JOIN blocks b
        |WHERE b.height $comparator lastSeenHeight
        |ORDER BY height $order
        |LIMIT {limit}
      """.stripMargin
    ).on(
      'lastSeenHash -> lastSeenHash.string,
      'limit -> limit.int
    ).as(parseHeader.*)
  }

  private def toSQL(condition: OrderingCondition): String = condition match {
    case OrderingCondition.AscendingOrder => "ASC"
    case OrderingCondition.DescendingOrder => "DESC"
  }
}
