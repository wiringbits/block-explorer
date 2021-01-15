package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.executors.{
  DatabaseAkkaExecutionContext,
  DatabaseExecutionContext,
  ExternalServiceAkkaExecutionContext,
  ExternalServiceExecutionContext
}

class ExecutorsModule extends AbstractModule {

  override def configure(): Unit = {
    val _ = (
      bind(classOf[ExternalServiceExecutionContext]).to(
        classOf[ExternalServiceAkkaExecutionContext]
      ),
      bind(classOf[DatabaseExecutionContext]).to(
        classOf[DatabaseAkkaExecutionContext]
      )
    )
  }
}
