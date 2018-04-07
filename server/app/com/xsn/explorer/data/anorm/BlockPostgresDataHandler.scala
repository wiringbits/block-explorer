package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.BlockBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.BlockPostgresDAO
import com.xsn.explorer.errors.BlockUnknownError
import com.xsn.explorer.models.rpc.Block
import org.scalactic.{One, Or}
import play.api.db.Database

class BlockPostgresDataHandler @Inject() (
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO)
    extends BlockBlockingDataHandler
    with AnormPostgresDataHandler {

  override def create(block: Block): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.create(block)
    Or.from(maybe, One(BlockUnknownError))
  }
}
