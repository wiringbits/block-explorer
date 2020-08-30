package com.xsn.explorer.services.synchronizer

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.xsn.explorer.models._
import com.xsn.explorer.models.values.IPAddress

class MasternodeSynchronizerActor extends Actor {
  import context._

  def receive: Receive = {
    val initialMasternodes = List[rpc.Masternode]()

    behavior(initialMasternodes)
  }

  private def behavior(masternodes: List[rpc.Masternode]): Receive = {
    case MasternodeSynchronizerActor.UpdateMasternodes(newMasternodes) =>
      become(behavior(newMasternodes))
    case MasternodeSynchronizerActor.GetMasternode(ipAddress) =>
      sender() ! masternodes.find(x => x.ip.split(":").headOption.contains(ipAddress.string))
    case MasternodeSynchronizerActor.GetMasternodes =>
      sender() ! masternodes
  }
}

object MasternodeSynchronizerActor {
  final class Ref private (val ref: ActorRef) extends AnyVal

  object Ref {

    def apply(name: String = "masternode-synchronizer")(implicit system: ActorSystem): Ref = {
      val actor = system.actorOf(Props(new MasternodeSynchronizerActor), name)
      new Ref(actor)
    }
  }

  final case class UpdateMasternodes(masternodes: List[rpc.Masternode])
  final case class GetMasternode(ipAddress: IPAddress)
  final case class GetMasternodes()
}
