package com.xsn.explorer.services

import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.values.TransactionId

package object synchronizer {

  private[synchronizer] type BlockData = (Block.HasTransactions, List[TPoSContract], () => GolombCodedSet)

  private[synchronizer] val ExcludedTransactions: List[TransactionId] =
    List("e3bf3d07d4b0375638d5f1db5255fe07ba2c4cb067cd81b84ee974b6585fb468").flatMap(TransactionId.from)

}
