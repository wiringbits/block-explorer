package com.xsn.explorer.services.synchronizer.repository

import akka.pattern.ask
import akka.util.Timeout
import com.xsn.explorer.services.synchronizer.NodeStatsSynchronizerActor
import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.Future

trait NodeStatsRepository {
  def getCoinsStaking(): Future[BigDecimal]
}

object NodeStatsRepository {
  private implicit val timeout: Timeout = 10.seconds

  class ActorImpl @Inject() (actor: NodeStatsSynchronizerActor.Ref) extends NodeStatsRepository {

    override def getCoinsStaking(): Future[BigDecimal] = {
      actor.ref
        .ask(NodeStatsSynchronizerActor.GetCoinsStaking)
        .mapTo[BigDecimal]
    }

  }
}
