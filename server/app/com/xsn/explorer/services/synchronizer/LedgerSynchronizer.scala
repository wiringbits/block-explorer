package com.xsn.explorer.services.synchronizer

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.models.values.Blockhash

trait LedgerSynchronizer {

  def synchronize(blockhash: Blockhash): FutureApplicationResult[Unit]
}
