package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.data.BlockBlockingDataHandler
import com.xsn.explorer.data.anorm.BlockPostgresDataHandler

class DataHandlerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[BlockBlockingDataHandler]).to(classOf[BlockPostgresDataHandler])
  }
}
