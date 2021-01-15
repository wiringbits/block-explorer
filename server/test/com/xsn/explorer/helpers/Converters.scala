package com.xsn.explorer.helpers

import com.xsn.explorer.models._

import scala.language.implicitConversions

object Converters {

  implicit def toPersistedBlock(
      rpcBlock: rpc.Block.Canonical
  ): persisted.Block = {
    transformers.toPersistedBlock(rpcBlock, BlockExtractionMethod.ProofOfWork)
  }
}
