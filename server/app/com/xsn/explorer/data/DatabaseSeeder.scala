package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.Transaction
import com.xsn.explorer.models.rpc.Block

import scala.language.higherKinds

trait DatabaseSeeder[F[_]] {

  import DatabaseSeeder._

  /**
   * The database has some blocks, we are adding a new a block.
   */
  def newBlock(command: CreateBlockCommand): F[Unit]

  /**
   * The database has some blocks but there is a rechain happening, we need to
   * replace our current latest block with the new latest block.
   */
  def replaceBlock(command: ReplaceBlockCommand): F[Unit]
}

object DatabaseSeeder {

  case class CreateBlockCommand(block: Block, transactions: List[Transaction])
  case class ReplaceBlockCommand(
      orphanBlock: Block,
      newBlock: Block, newTransactions: List[Transaction])

}

trait DatabaseBlockingSeeder extends DatabaseSeeder[ApplicationResult]
