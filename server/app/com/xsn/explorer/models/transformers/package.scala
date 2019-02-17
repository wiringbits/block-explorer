package com.xsn.explorer.models

import com.xsn.explorer.models.persisted.Block
import io.scalaland.chimney.dsl._

/**
 * The package is a bridge between model domains, for example, sometimes you will have a rpc.Block but need a
 * persisted.Block, this is where the transormation logic lives.
 */
package object transformers {

  def toPersistedBlock(rpcBlock: rpc.Block): persisted.Block = {
    rpcBlock
        .into[Block]
        .withFieldConst(_.extractionMethod, Block.ExtractionMethod.ProofOfWork) // TODO: Get proper method
        .transform
  }
}
