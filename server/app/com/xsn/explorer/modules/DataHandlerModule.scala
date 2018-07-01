package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.data._
import com.xsn.explorer.data.anorm._

class DataHandlerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[BlockBlockingDataHandler]).to(classOf[BlockPostgresDataHandler])
    bind(classOf[BalanceBlockingDataHandler]).to(classOf[BalancePostgresDataHandler])
    bind(classOf[StatisticsBlockingDataHandler]).to(classOf[StatisticsPostgresDataHandler])
    bind(classOf[DatabaseBlockingSeeder]).to(classOf[DatabasePostgresSeeder])
    bind(classOf[TransactionBlockingDataHandler]).to(classOf[TransactionPostgresDataHandler])
    bind(classOf[LedgerBlockingDataHandler]).to(classOf[LedgerPostgresDataHandler])
  }
}
