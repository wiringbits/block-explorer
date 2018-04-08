package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.config.{PlayRPCConfig, PlaySeederConfig, RPCConfig, SeederConfig}

class ConfigModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[RPCConfig]).to(classOf[PlayRPCConfig])
    bind(classOf[SeederConfig]).to(classOf[PlaySeederConfig])
  }
}
