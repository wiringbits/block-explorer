package com.xsn.explorer.config

import com.xsn.explorer.models.Blockhash
import javax.inject.Inject
import play.api.Configuration

trait ExplorerConfig {

  def genesisBlock: Blockhash
}

class PlayExplorerConfig @Inject() (config: Configuration) extends ExplorerConfig {

  override val genesisBlock:  Blockhash = {
    Blockhash
      .from(config.get[String]("explorer.genesisBlock"))
      .getOrElse(throw new RuntimeException("The given genesisBlock is incorrect"))
  }

}

