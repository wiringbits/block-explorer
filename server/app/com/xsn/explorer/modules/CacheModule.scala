package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.cache.BlockHeaderCache

class CacheModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[BlockHeaderCache]).toInstance(BlockHeaderCache.default)
  }
}
