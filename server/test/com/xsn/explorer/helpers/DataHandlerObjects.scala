package com.xsn.explorer.helpers

import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.dao._
import com.xsn.explorer.data.anorm.{BlockPostgresDataHandler, LedgerPostgresDataHandler, TransactionPostgresDataHandler}
import play.api.db.Database

trait DataHandlerObjects {

  lazy val fieldOrderingSQLInterpreter = new FieldOrderingSQLInterpreter
  lazy val transactionInputDAO = new TransactionInputPostgresDAO
  lazy val transactionOutputDAO = new TransactionOutputPostgresDAO
  lazy val addressTransactionDetailsDAO = new AddressTransactionDetailsPostgresDAO
  lazy val transactionPostgresDAO = new TransactionPostgresDAO(
    transactionInputDAO,
    transactionOutputDAO,
    addressTransactionDetailsDAO,
    fieldOrderingSQLInterpreter)
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

  def createTransactionDataHandler(database: Database) = {
    new TransactionPostgresDataHandler(database, transactionOutputDAO, transactionPostgresDAO)
  }
}

object DataHandlerObjects extends DataHandlerObjects
