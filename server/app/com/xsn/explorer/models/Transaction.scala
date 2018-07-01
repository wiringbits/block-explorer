package com.xsn.explorer.models

case class Transaction(
    id: TransactionId,
    blockhash: Blockhash,
    time: Long,
    size: Size,
    inputs: List[Transaction.Input],
    outputs: List[Transaction.Output])

object Transaction {

  case class Input(
      index: Int,
      value: Option[BigDecimal],
      address: Option[Address])

  case class Output(
      index: Int,
      value: BigDecimal,
      address: Address,
      tposOwnerAddress: Option[Address],
      tposMerchantAddress: Option[Address])

  /**
   * Please note that the inputs might not be accurate.
   *
   * If the rpc transaction might not be complete, get the input value and address using
   * the utxo index or the getTransaction method from the TransactionService..
   */
  def fromRPC(tx: rpc.Transaction): Transaction = {
    val inputs = tx.vin.zipWithIndex.map { case (vin, index) =>
      Transaction.Input(index, vin.value, vin.address)
    }

    val outputs = tx.vout.flatMap { vout =>
      val tposAddresses = vout.scriptPubKey.flatMap(_.getTPoSAddresses)
      for {
        address <- vout.address
      } yield Transaction.Output(vout.n, vout.value, address, tposAddresses.map(_._1), tposAddresses.map(_._2))
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
