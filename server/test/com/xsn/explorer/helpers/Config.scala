package com.xsn.explorer.helpers

import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.models.values.Blockhash

object Config {

  val explorerConfig = new ExplorerConfig {

    override def genesisBlock: Blockhash =
      Blockhash.from("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34").get

    override def liteVersionConfig: ExplorerConfig.LiteVersionConfig =
      ExplorerConfig.LiteVersionConfig(enabled = false, 0)
  }
}
