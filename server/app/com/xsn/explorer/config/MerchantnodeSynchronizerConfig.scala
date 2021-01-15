package com.xsn.explorer.config

import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

case class MerchantnodeSynchronizerConfig(
    enabled: Boolean,
    initialDelay: FiniteDuration,
    interval: FiniteDuration
)

object MerchantnodeSynchronizerConfig {

  def apply(config: Configuration): MerchantnodeSynchronizerConfig = {
    val enabled: Boolean =
      config.get[Boolean]("merchantnodeSynchronizer.enabled")
    val initialDelay: FiniteDuration =
      config.get[FiniteDuration]("merchantnodeSynchronizer.initialDelay")
    val interval: FiniteDuration =
      config.get[FiniteDuration]("merchantnodeSynchronizer.interval")

    MerchantnodeSynchronizerConfig(enabled, initialDelay, interval)
  }
}
