package com.xsn.explorer.config

import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

case class NodeStatsSynchronizerConfig(enabled: Boolean, initialDelay: FiniteDuration, interval: FiniteDuration)

object NodeStatsSynchronizerConfig {

  def apply(config: Configuration): NodeStatsSynchronizerConfig = {
    val enabled: Boolean = config.get[Boolean]("nodeStatsSynchronizer.enabled")
    val initialDelay: FiniteDuration = config.get[FiniteDuration]("nodeStatsSynchronizer.initialDelay")
    val interval: FiniteDuration = config.get[FiniteDuration]("nodeStatsSynchronizer.interval")

    NodeStatsSynchronizerConfig(enabled, initialDelay, interval)
  }
}