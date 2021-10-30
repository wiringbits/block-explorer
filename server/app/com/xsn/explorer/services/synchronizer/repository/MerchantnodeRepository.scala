package com.xsn.explorer.services.synchronizer.repository

import akka.pattern.ask
import akka.util.Timeout
import com.xsn.explorer.models.rpc.Merchantnode
import com.xsn.explorer.services.synchronizer.MerchantnodeSynchronizerActor
import javax.inject.Inject
import scala.concurrent.duration._
import com.xsn.explorer.models.values.IPAddress
import scala.concurrent.Future

trait MerchantnodeRepository {
  def getAll(): Future[List[Merchantnode]]
  def find(ipAddress: IPAddress): Future[Option[Merchantnode]]
  def getCount(): Future[Int]
  def getEnabledCount(): Future[Int]
  def getProtocols(): Future[Map[String, Int]]
}

object MerchantnodeRepository {
  private implicit val timeout: Timeout = 10.seconds

  class ActorImpl @Inject() (actor: MerchantnodeSynchronizerActor.Ref) extends MerchantnodeRepository {
    override def getAll(): Future[List[Merchantnode]] = {
      actor.ref
        .ask(MerchantnodeSynchronizerActor.GetMerchantnodes)
        .mapTo[List[Merchantnode]]
    }

    override def find(ipAddress: IPAddress): Future[Option[Merchantnode]] = {
      actor.ref
        .ask(MerchantnodeSynchronizerActor.GetMerchantnode(ipAddress))
        .mapTo[Option[Merchantnode]]
    }

    override def getCount(): Future[Int] = {
      actor.ref
        .ask(MerchantnodeSynchronizerActor.GetMerchantnodeCount)
        .mapTo[Int]
    }

    override def getEnabledCount(): Future[Int] = {
      actor.ref
        .ask(MerchantnodeSynchronizerActor.GetEnabledMerchantnodeCount)
        .mapTo[Int]
    }

    override def getProtocols(): Future[Map[String, Int]] = {
      actor.ref
        .ask(MerchantnodeSynchronizerActor.GetMerchantnodeProtocols)
        .mapTo[Map[String, Int]]
    }
  }
}
