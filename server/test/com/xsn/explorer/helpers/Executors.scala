package com.xsn.explorer.helpers

import com.xsn.explorer.executors.ExternalServiceExecutionContext

import scala.concurrent.ExecutionContext

object Executors {

  implicit val globalEC: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit val externalServiceEC: ExternalServiceExecutionContext = new ExternalServiceExecutionContext {
    override def execute(runnable: Runnable): Unit = globalEC.execute(runnable)

    override def reportFailure(cause: Throwable): Unit = globalEC.reportFailure(cause)
  }
}
