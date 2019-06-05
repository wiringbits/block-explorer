package com.xsn.explorer.helpers

import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.dao._
import com.xsn.explorer.data.anorm.{BlockPostgresDataHandler, LedgerPostgresDataHandler, TransactionPostgresDataHandler}
import play.api.db.Database

trait DataHandlerObjects {

  import Config.explorerConfig

  lazy val fieldOrderingSQLInterpreter = new FieldOrderingSQLInterpreter
  lazy val transactionInputDAO = new TransactionInputPostgresDAO(explorerConfig)
  lazy val transactionOutputDAO = new TransactionOutputPostgresDAO(explorerConfig)
  lazy val addressTransactionDetailsDAO = new AddressTransactionDetailsPostgresDAO(explorerConfig)
  lazy val tposContractDAO = new TPoSContractDAO
  lazy val transactionPostgresDAO = new TransactionPostgresDAO(
    explorerConfig,
    transactionInputDAO,
    transactionOutputDAO,
    tposContractDAO,
    addressTransactionDetailsDAO,
    fieldOrderingSQLInterpreter
  )
  lazy val blockFilterPostgresDAO = new BlockFilterPostgresDAO
  lazy val blockPostgresDAO = new BlockPostgresDAO(blockFilterPostgresDAO, fieldOrderingSQLInterpreter)
  lazy val balancePostgresDAO = new BalancePostgresDAO(fieldOrderingSQLInterpreter)
  lazy val aggregatedAmountPostgresDAO = new AggregatedAmountPostgresDAO

  def createLedgerDataHandler(database: Database) = {
    new LedgerPostgresDataHandler(
      database,
      blockPostgresDAO,
      blockFilterPostgresDAO,
      transactionPostgresDAO,
      balancePostgresDAO,
      aggregatedAmountPostgresDAO
    )
  }

  def createBlockDataHandler(database: Database) = {
    new BlockPostgresDataHandler(database, blockPostgresDAO)
  }

  def createTransactionDataHandler(database: Database) = {
    new TransactionPostgresDataHandler(database, transactionOutputDAO, transactionPostgresDAO)
  }
}

object DataHandlerObjects extends DataHandlerObjects
