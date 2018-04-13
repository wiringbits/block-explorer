package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, BlockPostgresDataHandler, StatisticsPostgresDataHandler}
import com.xsn.explorer.data.{BalanceBlockingDataHandler, BlockBlockingDataHandler, StatisticsBlockingDataHandler}

class DataHandlerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[BlockBlockingDataHandler]).to(classOf[BlockPostgresDataHandler])
    bind(classOf[BalanceBlockingDataHandler]).to(classOf[BalancePostgresDataHandler])
    bind(classOf[StatisticsBlockingDataHandler]).to(classOf[StatisticsPostgresDataHandler])
  }
}
