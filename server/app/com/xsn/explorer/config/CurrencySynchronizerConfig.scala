package com.xsn.explorer.config

import javax.inject.Inject
import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

trait CurrencySynchronizerConfig {

  def enabled: Boolean
  def initialDelay: FiniteDuration
  def interval: FiniteDuration
}

class PlayCurrencySynchronizerConfig @Inject()(config: Configuration) extends CurrencySynchronizerConfig {

  override val enabled: Boolean = config.get[Boolean]("currencySynchronizer.enabled")

  override def initialDelay: FiniteDuration = config.get[FiniteDuration]("currencySynchronizer.initialDelay")

  override def interval: FiniteDuration = config.get[FiniteDuration]("currencySynchronizer.interval")

}
