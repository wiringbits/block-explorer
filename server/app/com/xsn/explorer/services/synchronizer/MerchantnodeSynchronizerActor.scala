package com.xsn.explorer.services.synchronizer

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.xsn.explorer.models._
import com.xsn.explorer.models.values.IPAddress

class MerchantnodeSynchronizerActor extends Actor {
  import context._

  def receive: Receive = {
    val initialMerchantnodes = List[rpc.Merchantnode]()

    behavior(initialMerchantnodes)
  }

  private def behavior(merchantnodes: List[rpc.Merchantnode]): Receive = {
    case MerchantnodeSynchronizerActor.UpdateMerchantnodes(newMerchantnodes) =>
      become(behavior(newMerchantnodes))
    case MerchantnodeSynchronizerActor.GetMerchantnode(ipAddress) =>
      sender() ! merchantnodes.find(x => x.ip.split(":").headOption.contains(ipAddress.string))
    case MerchantnodeSynchronizerActor.GetMerchantnodes =>
      sender() ! merchantnodes
    case MerchantnodeSynchronizerActor.GetMerchantnodeCount =>
      sender() ! merchantnodes.length
  }
}

object MerchantnodeSynchronizerActor {
  final class Ref private (val ref: ActorRef)

  object Ref {

    def apply(name: String = "merchantnode-synchronizer")(implicit system: ActorSystem): Ref = {
      val actor = system.actorOf(Props(new MerchantnodeSynchronizerActor), name)
      new Ref(actor)
    }
  }

  final case class UpdateMerchantnodes(merchantnodes: List[rpc.Merchantnode])
  final case class GetMerchantnode(ipAddress: IPAddress)
  final case class GetMerchantnodes()
  final case class GetMerchantnodeCount()
}