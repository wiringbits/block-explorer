package com.xsn.explorer.services.synchronizer.repository

import akka.pattern.ask
import akka.util.Timeout
import com.xsn.explorer.models.rpc.Masternode
import com.xsn.explorer.services.synchronizer.MasternodeSynchronizerActor
import javax.inject.Inject
import scala.concurrent.duration._
import com.xsn.explorer.models.values.IPAddress
import scala.concurrent.Future

trait MasternodeRepository {
  def getAll(): Future[List[Masternode]]
  def find(ipAddress: IPAddress): Future[Option[Masternode]]
  def getCount(): Future[Int]
  def getEnabledCount(): Future[Int]
  def getProtocols(): Future[Map[String, Int]]
}

object MasternodeRepository {
  private implicit val timeout: Timeout = 10.seconds

  class ActorImpl @Inject() (actor: MasternodeSynchronizerActor.Ref)
      extends MasternodeRepository {
    override def getAll(): Future[List[Masternode]] = {
      actor.ref
        .ask(MasternodeSynchronizerActor.GetMasternodes)
        .mapTo[List[Masternode]]
    }

    override def find(ipAddress: IPAddress): Future[Option[Masternode]] = {
      actor.ref
        .ask(MasternodeSynchronizerActor.GetMasternode(ipAddress))
        .mapTo[Option[Masternode]]
    }

    override def getCount(): Future[Int] = {
      actor.ref.ask(MasternodeSynchronizerActor.GetMasternodeCount).mapTo[Int]
    }

    override def getEnabledCount(): Future[Int] = {
      actor.ref
        .ask(MasternodeSynchronizerActor.GetEnabledMasternodeCount)
        .mapTo[Int]
    }

    override def getProtocols(): Future[Map[String, Int]] = {
      actor.ref
        .ask(MasternodeSynchronizerActor.GetMasternodeProtocols)
        .mapTo[Map[String, Int]]
    }
  }
}
