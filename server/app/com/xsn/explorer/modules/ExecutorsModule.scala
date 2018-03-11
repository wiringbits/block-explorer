package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.executors.{ExternalServiceAkkaExecutionContext, ExternalServiceExecutionContext}

class ExecutorsModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ExternalServiceExecutionContext]).to(classOf[ExternalServiceAkkaExecutionContext])
  }
}
