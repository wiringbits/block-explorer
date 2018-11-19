package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.config._

class ConfigModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[RPCConfig]).to(classOf[PlayRPCConfig])
    bind(classOf[LedgerSynchronizerConfig]).to(classOf[LedgerSynchronizerPlayConfig])
    bind(classOf[ExplorerConfig]).to(classOf[PlayExplorerConfig])
  }
}
