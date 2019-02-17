package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.values._
import com.xsn.explorer.models.rpc

case class Transaction(
    id: TransactionId,
    blockhash: Blockhash,
    time: Long,
    size: Size,
    inputs: List[Transaction.Input],
    outputs: List[Transaction.Output]) {

  require(
    outputs.forall(_.txid == id),
    "There are outputs that having a different txid"
  )
}

object Transaction {

  /**
   * The coins where generated on the given output index of the given txid (from).
   */
  case class Input(
      fromTxid: TransactionId,
      fromOutputIndex: Int,
      index: Int,
      value: BigDecimal,
      address: Address)

  case class Output(
      txid: TransactionId,
      index: Int,
      value: BigDecimal,
      address: Address,
      script: HexString,
      tposOwnerAddress: Option[Address],
      tposMerchantAddress: Option[Address])

  /**
   * Please note that the inputs might not be accurate.
   *
   * If the rpc transaction might not be complete, get the input value and address using
   * the utxo index or the getTransaction method from the TransactionService..
   */
  def fromRPC(tx: rpc.Transaction): Transaction = {
    val inputs = tx.vin.zipWithIndex.flatMap { case (vin, index) =>
      for {
        value <- vin.value
        address <- vin.address
      } yield Transaction.Input(vin.txid, vin.voutIndex, index, value, address)
    }

    val outputs = tx.vout.flatMap { vout =>
      val tposAddresses = vout.scriptPubKey.flatMap(_.getTPoSAddresses)
      val scriptMaybe = vout.scriptPubKey.map(_.hex)
      for {
        address <- vout.address
        script <- scriptMaybe
      } yield Transaction.Output(tx.id, vout.n, vout.value, address, script, tposAddresses.map(_._1), tposAddresses.map(_._2))
    }

    Transaction(
      id = tx.id,
      blockhash = tx.blockhash,
      time = tx.time,
      size = tx.size,
      inputs = inputs,
      outputs = outputs
    )
  }
}
