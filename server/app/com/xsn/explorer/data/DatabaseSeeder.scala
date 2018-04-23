package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.Transaction
import com.xsn.explorer.models.rpc.Block

import scala.language.higherKinds

trait DatabaseSeeder[F[_]] {

  import DatabaseSeeder._

  /**
   * There are no blocks, we are adding the first one which could possibly
   * not be the genesis block but the first one on our database.
   */
  def firstBlock(command: CreateBlockCommand): F[Unit]

  /**
   * The database has some blocks, we are appending a block to our latest block.
   */
  def newLatestBlock(command: CreateBlockCommand): F[Unit]

  /**
   * The database has some blocks but there is a rechain happening, we need to
   * replace our current latest block with the new latest block.
   */
  def replaceLatestBlock(command: ReplaceBlockCommand): F[Unit]

  /**
   * The database has some blocks but the chain is not complete, we are inserting
   * a previous block that's missing in our chain.
   */
  def insertPendingBlock(command: CreateBlockCommand): F[Unit]
}

object DatabaseSeeder {

  case class CreateBlockCommand(block: Block, transactions: List[Transaction])
  case class ReplaceBlockCommand(
      orphanBlock: Block,
      newBlock: Block, newTransactions: List[Transaction])

}

trait DatabaseBlockingSeeder extends DatabaseSeeder[ApplicationResult]
