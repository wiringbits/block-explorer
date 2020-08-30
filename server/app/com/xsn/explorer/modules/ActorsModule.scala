package com.xsn.explorer.modules

import com.google.inject.{AbstractModule, Provides}
import javax.inject.Singleton
import akka.actor.ActorSystem
import com.xsn.explorer.services.synchronizer.MasternodeSynchronizerActor
import com.xsn.explorer.services.synchronizer.repository.MasternodeRepository

class ActorsModule extends AbstractModule {

  @Provides
  @Singleton
  def masternodeSynchronizerActor()(implicit actorSystem: ActorSystem): MasternodeSynchronizerActor.Ref = {
    MasternodeSynchronizerActor.Ref.apply()
  }

  override def configure(): Unit = {
    val _ = bind(classOf[MasternodeRepository]).to(classOf[MasternodeRepository.ActorImpl])
  }
}
