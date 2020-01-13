package com.xsn.explorer.config

import com.xsn.explorer.config.CoinMarketCapConfig._
import play.api.Configuration

case class CoinMarketCapConfig(host: Host, key: Key, coinID: CoinID)

object CoinMarketCapConfig {

  case class Host(string: String) extends AnyVal
  case class Key(string: String) extends AnyVal
  case class CoinID(string: String) extends AnyVal

  def apply(config: Configuration): CoinMarketCapConfig = {
    val host = Host(config.get[String]("coinMarketCap.host"))
    val key = Key(config.get[String]("coinMarketCap.key"))
    val coinID = CoinID(config.get[String]("coinMarketCap.coinID"))

    CoinMarketCapConfig(host, key, coinID)
  }
}
