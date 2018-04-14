package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, BlockPostgresDataHandler, DatabasePostgresSeeder, StatisticsPostgresDataHandler}
import com.xsn.explorer.data.{BalanceBlockingDataHandler, BlockBlockingDataHandler, DatabaseBlockingSeeder, StatisticsBlockingDataHandler}

class DataHandlerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[BlockBlockingDataHandler]).to(classOf[BlockPostgresDataHandler])
    bind(classOf[BalanceBlockingDataHandler]).to(classOf[BalancePostgresDataHandler])
    bind(classOf[StatisticsBlockingDataHandler]).to(classOf[StatisticsPostgresDataHandler])
    bind(classOf[DatabaseBlockingSeeder]).to(classOf[DatabasePostgresSeeder])
  }
}
