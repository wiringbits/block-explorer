package com.xsn.explorer.modules

import com.google.inject.{AbstractModule, Provides}
import com.xsn.explorer.config._
import play.api.Configuration

class ConfigModule extends AbstractModule {

  override def configure(): Unit = {
    val _ = (
      bind(classOf[RPCConfig]).to(classOf[PlayRPCConfig]),
      bind(classOf[LedgerSynchronizerConfig]).to(classOf[LedgerSynchronizerPlayConfig]),
      bind(classOf[ExplorerConfig]).to(classOf[ExplorerConfig.Play])
    )
  }

  @Provides
  def coinMarketCapConfig(config: Configuration): CoinMarketCapConfig = CoinMarketCapConfig(config)

  @Provides
  def currencuSynchronizerConfig(config: Configuration): CurrencySynchronizerConfig = CurrencySynchronizerConfig(config)
}
