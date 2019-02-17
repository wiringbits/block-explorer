package com.xsn.explorer.helpers

import com.xsn.explorer.models._
import io.scalaland.chimney.dsl._

import scala.language.implicitConversions

object Converters {

  implicit def toPersistedBlock(rpcBlock: rpc.Block): persisted.Block = {
    rpcBlock
        .into[persisted.Block]
        .withFieldConst(_.extractionMethod, persisted.Block.ExtractionMethod.ProofOfWork) // TODO: Detect method
        .transform
  }
}
