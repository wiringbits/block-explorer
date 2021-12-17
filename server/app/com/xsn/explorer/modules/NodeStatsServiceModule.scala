package com.xsn.explorer.modules

import com.google.inject.{AbstractModule, Provides}
import javax.inject.Singleton
import akka.actor.ActorSystem
import com.xsn.explorer.services.synchronizer.NodeStatsSynchronizerActor
import com.xsn.explorer.services.synchronizer.repository.NodeStatsRepository

class NodeStatsServiceModule extends AbstractModule {

  @Provides
  @Singleton
  def nodeStatsUpdaterActor()(implicit
      actorSystem: ActorSystem
  ): NodeStatsSynchronizerActor.Ref = {
    NodeStatsSynchronizerActor.Ref.apply()
  }

  override def configure(): Unit = {
    val _ = bind(classOf[NodeStatsRepository]).to(
      classOf[NodeStatsRepository.ActorImpl]
    )
  }
}
