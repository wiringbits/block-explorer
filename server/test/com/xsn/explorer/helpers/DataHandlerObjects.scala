package com.xsn.explorer.helpers

import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.dao.{AggregatedAmountPostgresDAO, BalancePostgresDAO, BlockPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.data.anorm.{BlockPostgresDataHandler, LedgerPostgresDataHandler}
import play.api.db.Database

trait DataHandlerObjects {

  lazy val fieldOrderingSQLInterpreter = new FieldOrderingSQLInterpreter
  lazy val transactionPostgresDAO = new TransactionPostgresDAO(fieldOrderingSQLInterpreter)
  lazy val blockPostgresDAO = new BlockPostgresDAO(fieldOrderingSQLInterpreter)
  lazy val balancePostgresDAO = new BalancePostgresDAO(fieldOrderingSQLInterpreter)
  lazy val aggregatedAmountPostgresDAO = new AggregatedAmountPostgresDAO

  def createLedgerDataHandler(database: Database) = {
    new LedgerPostgresDataHandler(
      database,
      blockPostgresDAO,
      transactionPostgresDAO,
      balancePostgresDAO,
      aggregatedAmountPostgresDAO)
  }

  def createBlockDataHandler(database: Database) = {
    new BlockPostgresDataHandler(database, blockPostgresDAO)
  }
}

object DataHandlerObjects extends DataHandlerObjects
