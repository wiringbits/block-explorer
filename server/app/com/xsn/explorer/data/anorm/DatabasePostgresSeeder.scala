package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.anorm.dao.BlockPostgresDAO
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block
import org.scalactic.Good
import play.api.db.Database

class DatabasePostgresSeeder @Inject() (
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO)
    extends AnormPostgresDataHandler {

  def firstBlock(block: Block): ApplicationResult[Unit] = database.withConnection { implicit conn =>
    val result = blockPostgresDAO.upsert(block)

    result
        .map(_ => Good(()))
        .getOrElse(throw new RuntimeException("Unable to add the first block"))
  }

  /**
   * Creates the new latest block assuming there is a previous block.
   *
   * @param newBlock
   * @return
   */
  def newLatestBlock(newBlock: Block): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val insertedBlock = for {
      _ <- blockPostgresDAO.upsert(newBlock)
    } yield ()

    val result = insertedBlock
        .flatMap(_ => newBlock.previousBlockhash)
        .flatMap { previousBlockhash =>

          for {
            previous <- blockPostgresDAO.getBy(previousBlockhash)
            newPrevious = previous.copy(nextBlockhash = Some(newBlock.hash))
            _ <- blockPostgresDAO.upsert(newPrevious)
          } yield ()
        }

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to add the new latest block"))
  }

  def replaceLatestBlock(newBlock: Block, orphan: Blockhash): ApplicationResult[Unit] = withTransaction { implicit conn =>
    val result = for {
      _ <- blockPostgresDAO.upsert(newBlock)
      _ <- blockPostgresDAO.delete(orphan)
    } yield ()

    result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to replace latest block"))
  }
}
