package com.xsn.explorer.models

import com.xsn.explorer.models.persisted.{Block, Transaction}
import io.scalaland.chimney.dsl._

/** The package is a bridge between model domains, for example, sometimes you
  * will have a rpc.Block but need a persisted.Block, this is where the
  * transormation logic lives.
  */
package object transformers {

  def toPersistedBlock(
      rpcBlock: rpc.Block[_],
      extractionMethod: BlockExtractionMethod
  ): persisted.Block = {
    rpcBlock
      .into[Block]
      .withFieldConst(_.extractionMethod, extractionMethod)
      .transform
  }

  def toLightWalletTransactionInput(
      input: Transaction.Input
  ): LightWalletTransaction.Input = {
    input
      .into[LightWalletTransaction.Input]
      .withFieldRenamed(_.fromOutputIndex, _.index)
      .withFieldRenamed(_.fromTxid, _.txid)
      .transform
  }

  def toLightWalletTransactionOutput(
      output: Transaction.Output
  ): LightWalletTransaction.Output = {
    output.into[LightWalletTransaction.Output].transform
  }

  def toLightWalletTransaction(
      tx: Transaction.HasIO
  ): LightWalletTransaction = {
    val inputs = tx.inputs.map(toLightWalletTransactionInput)
    val outputs = tx.outputs.map(toLightWalletTransactionOutput)

    tx.transaction
      .into[LightWalletTransaction]
      .withFieldConst(_.inputs, inputs)
      .withFieldConst(_.outputs, outputs)
      .transform
  }
}
