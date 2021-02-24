package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.services.{
  CurrencyService,
  CurrencyServiceCoinMarketCapImpl
}

class CurrencyServiceModule extends AbstractModule {

  override def configure(): Unit = {
    val _ = bind(classOf[CurrencyService]).to(
      classOf[CurrencyServiceCoinMarketCapImpl]
    )
  }
}
