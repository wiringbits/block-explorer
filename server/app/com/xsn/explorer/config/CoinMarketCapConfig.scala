package com.xsn.explorer.config

import javax.inject.Inject
import play.api.Configuration

trait CoinMarketCapConfig {

  import CoinMarketCapConfig._

  def host: Host
  def key: Key
  def coinID: CoinID
}

object CoinMarketCapConfig {

  case class Host(string: String) extends AnyVal
  case class Key(string: String) extends AnyVal
  case class CoinID(string: String) extends AnyVal
}

class PlayCoinMarketCapConfig @Inject()(config: Configuration) extends CoinMarketCapConfig {

  import CoinMarketCapConfig._

  private def get(name: String) = config.get[String](s"coinMarketCap.$name")

  override val host: Host = Host(get("host"))

  override def key: Key = Key(get("key"))

  override def coinID: CoinID = CoinID(get("coinID"))

}
