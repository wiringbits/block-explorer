package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.rpc
import com.xsn.explorer.models.rpc.TransactionVIN
import com.xsn.explorer.models.values._

case class Transaction(
    id: TransactionId,
    blockhash: Blockhash,
    time: Long,
    size: Size)

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
      script: HexString)

  case class HasIO(
      transaction: Transaction,
      inputs: List[Transaction.Input],
      outputs: List[Transaction.Output]) {

    require(
      outputs.forall(_.txid == transaction.id),
      "There are outputs that having a different txid"
    )

    def id: TransactionId = transaction.id
    def time: Long = transaction.time
    def blockhash: Blockhash = transaction.blockhash
    def size: Size = transaction.size
  }

  /**
   * Please note that the inputs might not be accurate.
   *
   * If the rpc transaction might not be complete, get the input value and address using
   * the utxo index or the getTransaction method from the TransactionService.
   */
  def fromRPC[VIN <: TransactionVIN](tx: rpc.Transaction[VIN]): HasIO = {
    val inputs = tx
        .vin
        .zipWithIndex
        .collect { case (vin: rpc.TransactionVIN.HasValues, index) => (vin, index) }
        .map { case (vin, index) =>
          Transaction.Input(vin.txid, vin.voutIndex, index, vin.value, vin.address)
        }

    val outputs = tx.vout.flatMap { vout =>
      val scriptMaybe = vout.scriptPubKey.map(_.hex)
      for {
        address <- vout.address
        script <- scriptMaybe
      } yield Transaction.Output(
        tx.id,
        vout.n,
        vout.value,
        address,
        script)
    }

    val transaction = Transaction(
      id = tx.id,
      blockhash = tx.blockhash,
      time = tx.time,
      size = tx.size
    )

    HasIO(transaction, inputs, outputs)
  }
}
