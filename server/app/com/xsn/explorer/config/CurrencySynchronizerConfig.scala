package com.xsn.explorer.config

import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

case class CurrencySynchronizerConfig(
    enabled: Boolean,
    initialDelay: FiniteDuration,
    highPriorityInterval: FiniteDuration,
    lowPriorityInterval: FiniteDuration
)

object CurrencySynchronizerConfig {

  def apply(config: Configuration): CurrencySynchronizerConfig = {
    val enabled: Boolean = config.get[Boolean]("currencySynchronizer.enabled")
    val initialDelay: FiniteDuration = config.get[FiniteDuration]("currencySynchronizer.initialDelay")
    val highPriorityInterval: FiniteDuration = config.get[FiniteDuration]("currencySynchronizer.highPriorityInterval")
    val lowPriorityInterval: FiniteDuration = config.get[FiniteDuration]("currencySynchronizer.lowPriorityInterval")

    CurrencySynchronizerConfig(enabled, initialDelay, highPriorityInterval, lowPriorityInterval)
  }
}
