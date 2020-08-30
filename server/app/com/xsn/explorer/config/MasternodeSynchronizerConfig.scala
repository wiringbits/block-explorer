package com.xsn.explorer.config

import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

case class MasternodeSynchronizerConfig(enabled: Boolean, initialDelay: FiniteDuration, interval: FiniteDuration)

object MasternodeSynchronizerConfig {

  def apply(config: Configuration): MasternodeSynchronizerConfig = {
    val enabled: Boolean = config.get[Boolean]("masternodeSynchronizer.enabled")
    val initialDelay: FiniteDuration = config.get[FiniteDuration]("masternodeSynchronizer.initialDelay")
    val interval: FiniteDuration = config.get[FiniteDuration]("masternodeSynchronizer.interval")

    MasternodeSynchronizerConfig(enabled, initialDelay, interval)
  }
}
