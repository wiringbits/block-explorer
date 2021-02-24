package com.xsn.explorer.executors

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext

import scala.concurrent.ExecutionContext

trait DatabaseExecutionContext extends ExecutionContext

@Singleton
class DatabaseAkkaExecutionContext @Inject() (system: ActorSystem)
    extends CustomExecutionContext(system, "database.dispatcher")
    with DatabaseExecutionContext
