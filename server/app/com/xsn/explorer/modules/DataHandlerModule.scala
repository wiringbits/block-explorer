package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, BlockPostgresDataHandler}
import com.xsn.explorer.data.{BalanceBlockingDataHandler, BlockBlockingDataHandler}

class DataHandlerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[BlockBlockingDataHandler]).to(classOf[BlockPostgresDataHandler])
    bind(classOf[BalanceBlockingDataHandler]).to(classOf[BalancePostgresDataHandler])
  }
}
