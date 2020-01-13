package com.xsn.explorer.config

import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

case class CurrencySynchronizerConfig(enabled: Boolean, initialDelay: FiniteDuration, interval: FiniteDuration)

object CurrencySynchronizerConfig {

  def apply(config: Configuration): CurrencySynchronizerConfig = {
    val enabled: Boolean = config.get[Boolean]("currencySynchronizer.enabled")
    val initialDelay: FiniteDuration = config.get[FiniteDuration]("currencySynchronizer.initialDelay")
    val interval: FiniteDuration = config.get[FiniteDuration]("currencySynchronizer.interval")

    CurrencySynchronizerConfig(enabled, initialDelay, interval)
  }
}
