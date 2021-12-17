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
    case MasternodeSynchronizerActor.GetMasternodeCount =>
      sender() ! masternodes.length
    case MasternodeSynchronizerActor.GetEnabledMasternodeCount =>
      sender() ! masternodes.count(x => x.status == "ENABLED")
    case MasternodeSynchronizerActor.GetMasternodeProtocols =>
      sender() ! masternodes.groupBy(_.protocol).view.mapValues(_.length).toMap
  }
}

object MasternodeSynchronizerActor {
  final class Ref private (val ref: ActorRef)

  object Ref {

    def apply(
        name: String = "masternode-synchronizer"
    )(implicit system: ActorSystem): Ref = {
      val actor = system.actorOf(Props(new MasternodeSynchronizerActor), name)
      new Ref(actor)
    }
  }

  final case class UpdateMasternodes(masternodes: List[rpc.Masternode])
  final case class GetMasternode(ipAddress: IPAddress)
  final case class GetMasternodes()
  final case class GetMasternodeCount()
  final case class GetEnabledMasternodeCount()
  final case class GetMasternodeProtocols()
}
