package com.xsn.explorer.services.synchronizer.repository

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import com.xsn.explorer.data.anorm.parsers.CommonParsers._
import com.xsn.explorer.models.values.Blockhash
import com.xsn.explorer.services.synchronizer.BlockSynchronizationState

class BlockSynchronizationProgressDAO {

  def upsert(blockhash: Blockhash, state: BlockSynchronizationState)(implicit conn: Connection): Unit = {
    val _ = SQL(
      """
        |INSERT INTO block_synchronization_progress
        |  (blockhash, state)
        |VALUES
        |  ({blockhash}, {state}::BLOCK_SYNCHRONIZATION_STATE)
        |ON CONFLICT (blockhash) DO UPDATE
        |  SET state = EXCLUDED.state
      """.stripMargin
    ).on("blockhash" -> blockhash.toBytesBE.toArray, "state" -> state.entryName).execute()
  }

  def find(blockhash: Blockhash)(implicit conn: Connection): Option[BlockSynchronizationState] = {
    val maybe = SQL(
      """
        |SELECT state
        |FROM block_synchronization_progress
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on("blockhash" -> blockhash.toBytesBE.toArray).as(scalar[String].singleOpt)

    maybe.map(BlockSynchronizationState.withNameInsensitive)
  }

  def findAny(implicit conn: Connection): Option[Blockhash] = {
    SQL(
      """
        |SELECT blockhash
        |FROM block_synchronization_progress
        |LIMIT 1
      """.stripMargin
    ).as(parseBlockhashBytes().singleOpt)
  }

  def delete(blockhash: Blockhash)(implicit conn: Connection): Unit = {
    val _ = SQL(
      """
        |DELETE FROM block_synchronization_progress
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on("blockhash" -> blockhash.toBytesBE.toArray).execute()
  }
}
