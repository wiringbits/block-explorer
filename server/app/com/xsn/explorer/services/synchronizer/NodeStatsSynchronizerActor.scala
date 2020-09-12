package com.xsn.explorer.services.synchronizer

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

class NodeStatsSynchronizerActor extends Actor {
  import context._

  def receive: Receive = {
    val coinsStaking = 0

    behavior(coinsStaking)
  }

  private def behavior(coinsStaking: BigDecimal): Receive = {
    case NodeStatsSynchronizerActor.UpdateCoinsStaking(newCoinsStaking) =>
      become(behavior(newCoinsStaking))
    case NodeStatsSynchronizerActor.GetCoinsStaking =>
      sender() ! coinsStaking
  }
}

object NodeStatsSynchronizerActor {
  final class Ref private (val ref: ActorRef)

  object Ref {

    def apply(name: String = "node-stats-synchronizer")(implicit system: ActorSystem): Ref = {
      val actor = system.actorOf(Props(new NodeStatsSynchronizerActor), name)
      new Ref(actor)
    }
  }

  final case class UpdateCoinsStaking(coinsStaking: BigDecimal)
  final case class GetCoinsStaking()
}