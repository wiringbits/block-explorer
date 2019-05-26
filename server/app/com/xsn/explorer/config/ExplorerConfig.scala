package com.xsn.explorer.config

import com.xsn.explorer.models.values.Blockhash
import javax.inject.Inject
import play.api.Configuration

trait ExplorerConfig {

  def genesisBlock: Blockhash

  def liteVersionConfig: ExplorerConfig.LiteVersionConfig
}

object ExplorerConfig {

  case class LiteVersionConfig(enabled: Boolean, syncTransactionsFromBlock: Int)

  class Play @Inject()(config: Configuration) extends ExplorerConfig {

    override val genesisBlock: Blockhash = {
      Blockhash
        .from(config.get[String]("explorer.genesisBlock"))
        .getOrElse(throw new RuntimeException("The given genesisBlock is incorrect"))
    }

    override def liteVersionConfig: LiteVersionConfig = {
      val inner = config.get[Configuration]("explorer.liteVersion")
      val enabled = inner.get[Boolean]("enabled")
      val syncTransactionsFromBlock = inner.get[Int]("syncTransactionsFromBlock")

      LiteVersionConfig(enabled, syncTransactionsFromBlock)
    }
  }
}
