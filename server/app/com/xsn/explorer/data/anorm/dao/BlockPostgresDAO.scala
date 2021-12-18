package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Count, Limit, Offset, PaginatedQuery}
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.parsers.BlockParsers._
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.persisted.{Block, BlockHeader, BlockInfo}
import com.xsn.explorer.models.values.{Blockhash, Height}
import javax.inject.Inject

class BlockPostgresDAO @Inject() (
    blockFilterPostgresDAO: BlockFilterPostgresDAO,
    fieldOrderingSQLInterpreter: FieldOrderingSQLInterpreter
) {

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
      "blockhash" -> block.hash.toBytesBE.toArray,
      "previous_blockhash" -> block.previousBlockhash.map(_.toBytesBE.toArray),
      "next_blockhash" -> block.nextBlockhash.map(_.toBytesBE.toArray),
      "tpos_contract" -> block.tposContract.map(_.toBytesBE.toArray),
      "merkle_root" -> block.merkleRoot.toBytesBE.toArray,
      "size" -> block.size.int,
      "height" -> block.height.int,
      "version" -> block.version,
      "time" -> block.time,
      "median_time" -> block.medianTime,
      "nonce" -> block.nonce,
      "bits" -> block.bits,
      "chainwork" -> block.chainwork,
      "difficulty" -> block.difficulty,
      "extraction_method" -> block.extractionMethod.entryName
    ).as(parseBlock.singleOpt)
  }

  def upsert(block: Block)(implicit conn: Connection): Unit = {
    val _ = SQL(
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
        |ON CONFLICT (blockhash) DO UPDATE
        |SET blockhash = EXCLUDED.blockhash,
        |    previous_blockhash = EXCLUDED.previous_blockhash,
        |    next_blockhash = EXCLUDED.next_blockhash,
        |    tpos_contract = EXCLUDED.tpos_contract,
        |    merkle_root = EXCLUDED.merkle_root,
        |    size = EXCLUDED.size,
        |    height = EXCLUDED.height,
        |    version = EXCLUDED.version,
        |    time = EXCLUDED.time,
        |    median_time = EXCLUDED.median_time,
        |    nonce = EXCLUDED.nonce,
        |    bits = EXCLUDED.bits,
        |    chainwork = EXCLUDED.chainwork,
        |    difficulty = EXCLUDED.difficulty,
        |    extraction_method = EXCLUDED.extraction_method
      """.stripMargin
    ).on(
      "blockhash" -> block.hash.toBytesBE.toArray,
      "previous_blockhash" -> block.previousBlockhash.map(_.toBytesBE.toArray),
      "next_blockhash" -> block.nextBlockhash.map(_.toBytesBE.toArray),
      "tpos_contract" -> block.tposContract.map(_.toBytesBE.toArray),
      "merkle_root" -> block.merkleRoot.toBytesBE.toArray,
      "size" -> block.size.int,
      "height" -> block.height.int,
      "version" -> block.version,
      "time" -> block.time,
      "median_time" -> block.medianTime,
      "nonce" -> block.nonce,
      "bits" -> block.bits,
      "chainwork" -> block.chainwork,
      "difficulty" -> block.difficulty,
      "extraction_method" -> block.extractionMethod.entryName
    ).execute()
  }

  def setNextBlockhash(blockhash: Blockhash, nextBlockhash: Blockhash)(implicit
      conn: Connection
  ): Option[Block] = {

    SQL(
      """
        |UPDATE blocks
        |SET next_blockhash = {next_blockhash}
        |WHERE blockhash = {blockhash}
        |RETURNING blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |          height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray,
      "next_blockhash" -> nextBlockhash.toBytesBE.toArray
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
      "blockhash" -> blockhash.toBytesBE.toArray
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
      ordering: FieldOrdering[BlockField]
  )(implicit
      conn: Connection
  ): List[Block] = {

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
      "offset" -> paginatedQuery.offset.int,
      "limit" -> paginatedQuery.limit.int
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
      "blockhash" -> blockhash.toBytesBE.toArray
    ).as(parseBlock.singleOpt)
  }

  def getLatestBlock(implicit conn: Connection): Option[Block] = {
    val query = PaginatedQuery(Offset(0), Limit(1))
    val ordering =
      FieldOrdering(BlockField.Height, OrderingCondition.DescendingOrder)
    getBy(query, ordering).headOption
  }

  def getFirstBlock(implicit conn: Connection): Option[Block] = {
    val query = PaginatedQuery(Offset(0), Limit(1))
    val ordering =
      FieldOrdering(BlockField.Height, OrderingCondition.AscendingOrder)
    getBy(query, ordering).headOption
  }

  def getHeaders(limit: Limit, orderingCondition: OrderingCondition)(implicit
      conn: Connection
  ): List[BlockHeader] = {
    val order = toSQL(orderingCondition)

    val headers = SQL(
      s"""
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |FROM blocks
        |ORDER BY height $order
        |LIMIT {limit}
      """.stripMargin
    ).on(
      "limit" -> limit.int
    ).as(parseHeader.*)

    for {
      header <- headers
      filterMaybe = blockFilterPostgresDAO.getBy(header.hash)
    } yield filterMaybe
      .map(header.withFilter)
      .getOrElse(header)
  }

  def getHeaders(
      lastSeenHash: Blockhash,
      limit: Limit,
      orderingCondition: OrderingCondition
  )(implicit
      conn: Connection
  ): List[BlockHeader] = {
    val order = toSQL(orderingCondition)
    val comparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder => ">"
    }

    val headers = SQL(
      s"""
        |WITH CTE AS (
        |  SELECT height as lastSeenHeight
        |  FROM blocks
        |  WHERE blockhash = {lastSeenHash}
        |)
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |FROM CTE CROSS JOIN blocks b
        |WHERE b.height $comparator lastSeenHeight
        |ORDER BY height $order
        |LIMIT {limit}
      """.stripMargin
    ).on(
      "lastSeenHash" -> lastSeenHash.toBytesBE.toArray,
      "limit" -> limit.int
    ).as(parseHeader.*)

    for {
      header <- headers
      filterMaybe = blockFilterPostgresDAO.getBy(header.hash)
    } yield filterMaybe
      .map(header.withFilter)
      .getOrElse(header)
  }

  def getHeader(blockhash: Blockhash, includeFilter: Boolean)(implicit
      conn: Connection
  ): Option[BlockHeader] = {
    val blockMaybe = SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |FROM blocks
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray
    ).as(parseHeader.singleOpt)

    if (includeFilter) {
      blockMaybe.map(attachFilter)
    } else {
      blockMaybe
    }
  }

  def getHeader(height: Height, includeFilter: Boolean)(implicit
      conn: Connection
  ): Option[BlockHeader] = {
    val blockMaybe = SQL(
      """
        |SELECT blockhash, previous_blockhash, next_blockhash, tpos_contract, merkle_root, size,
        |       height, version, time, median_time, nonce, bits, chainwork, difficulty, extraction_method
        |FROM blocks
        |WHERE height = {height}
      """.stripMargin
    ).on(
      "height" -> height.int
    ).as(parseHeader.singleOpt)

    if (includeFilter) {
      blockMaybe.map(attachFilter)
    } else {
      blockMaybe
    }
  }

  def getBlocks(limit: Limit, orderingCondition: OrderingCondition)(implicit
      conn: Connection
  ): List[BlockInfo] = {
    val order = toSQL(orderingCondition)

    SQL(
      s"""
        |SELECT blk.*, COALESCE(tx.count, 0) transactions 
        |FROM (
        |   SELECT blockhash, previous_blockhash, next_blockhash, merkle_root, height, time, difficulty, tpos_contract,
        |          median_time
        |   FROM blocks
        |   ORDER BY height $order
        |   LIMIT {limit}
        |) blk
        |LEFT JOIN (
        |   SELECT blockhash, count(*) count 
        |   FROM transactions 
        |   GROUP BY blockhash
        |) tx 
        |ON blk.blockhash = tx.blockhash
      """.stripMargin
    ).on(
      "limit" -> limit.int
    ).as(parseBlockInfo.*)
  }

  def getBlocks(
      lastSeenHash: Blockhash,
      limit: Limit,
      orderingCondition: OrderingCondition
  )(implicit
      conn: Connection
  ): List[BlockInfo] = {
    val order = toSQL(orderingCondition)
    val comparator = orderingCondition match {
      case OrderingCondition.DescendingOrder => "<"
      case OrderingCondition.AscendingOrder => ">"
    }

    SQL(
      s"""
        |SELECT blk.*, COALESCE(tx.count, 0) transactions 
        |FROM (
        |   WITH CTE AS (
        |     SELECT height as lastSeenHeight
        |     FROM blocks
        |     WHERE blockhash = {lastSeenHash}
        |   )
        |   SELECT blockhash, previous_blockhash, next_blockhash, merkle_root, height, time, difficulty, tpos_contract,
        |          median_time
        |   FROM CTE CROSS JOIN blocks b
        |   WHERE b.height $comparator lastSeenHeight
        |   ORDER BY height $order
        |   LIMIT {limit}
        |) blk
        |LEFT JOIN (
        |   SELECT blockhash, count(*) count 
        |   FROM transactions 
        |   GROUP BY blockhash
        |) tx 
        |ON blk.blockhash = tx.blockhash
        |ORDER BY blk.height $order
      """.stripMargin
    ).on(
      "lastSeenHash" -> lastSeenHash.toBytesBE.toArray,
      "limit" -> limit.int
    ).as(parseBlockInfo.*)
  }

  def getBlock(
      blockhash: Blockhash
  )(implicit conn: Connection): Option[BlockInfo] = {
    SQL(
      """
        |SELECT blk.*, COALESCE(tx.count, 0) transactions 
        |FROM (
        |   SELECT blockhash, previous_blockhash, next_blockhash, merkle_root, height, time, difficulty, tpos_contract,
        |          median_time
        |   FROM blocks 
        |   WHERE blockhash = {blockhash}
        |) blk 
        |LEFT JOIN (
        |   SELECT blockhash, count(*) count 
        |   FROM transactions 
        |   GROUP BY blockhash
        |) tx
        |ON blk.blockhash = tx.blockhash
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray
    ).as(parseBlockInfo.singleOpt)
  }

  def getBlock(height: Height)(implicit conn: Connection): Option[BlockInfo] = {
    SQL(
      """
        |SELECT blk.*, COALESCE(tx.count, 0) transactions 
        |FROM (
        |   SELECT blockhash, previous_blockhash, next_blockhash, merkle_root, height, time, difficulty, tpos_contract,
        |          median_time
        |   FROM blocks 
        |   WHERE height = {height}
        |) blk 
        |LEFT JOIN (
        |   SELECT blockhash, count(*) count 
        |   FROM transactions 
        |   GROUP BY blockhash
        |) tx 
        |ON blk.blockhash = tx.blockhash
      """.stripMargin
    ).on(
      "height" -> height.int
    ).as(parseBlockInfo.singleOpt)
  }

  private def attachFilter(
      blockheader: BlockHeader
  )(implicit conn: Connection): BlockHeader = {
    val filterMaybe = blockFilterPostgresDAO.getBy(blockheader.hash)

    filterMaybe
      .map(blockheader.withFilter)
      .getOrElse(blockheader)
  }

  private def toSQL(condition: OrderingCondition): String = condition match {
    case OrderingCondition.AscendingOrder => "ASC"
    case OrderingCondition.DescendingOrder => "DESC"
  }
}
