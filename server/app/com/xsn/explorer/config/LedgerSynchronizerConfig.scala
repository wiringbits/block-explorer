package com.xsn.explorer.config

import javax.inject.Inject
import play.api.Configuration

import scala.concurrent.duration.{DurationLong, FiniteDuration}

trait LedgerSynchronizerConfig {

  def enabled: Boolean

  def initialDelay: FiniteDuration

  def interval: FiniteDuration

  def useNewSynchronizer: Boolean

  def parallelSynchronizer: Boolean
}

class LedgerSynchronizerPlayConfig @Inject()(config: Configuration) extends LedgerSynchronizerConfig {

  override lazy val enabled: Boolean = config.getOptional[Boolean]("synchronizer.enabled").getOrElse(false)

  override lazy val initialDelay: FiniteDuration =
    config.getOptional[FiniteDuration]("synchronizer.initialDelay").getOrElse(15.seconds)

  override lazy val interval: FiniteDuration =
    config.getOptional[FiniteDuration]("synchronizer.interval").getOrElse(60.seconds)

  override lazy val useNewSynchronizer: Boolean =
    config.getOptional[Boolean]("synchronizer.useNewSynchronizer").getOrElse(false)

  override lazy val parallelSynchronizer: Boolean = {
    config.getOptional[Boolean]("synchronizer.parallelSynchronizer").getOrElse(false)
  }
}
