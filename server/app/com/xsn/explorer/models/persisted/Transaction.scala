package com.xsn.explorer.models.persisted

import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.TransactionVIN
import com.xsn.explorer.models.values._

case class Transaction(
    id: TransactionId,
    blockhash: Blockhash,
    time: Long,
    size: Size
)

object Transaction {

  /** The coins where generated on the given output index of the given txid (from).
    */
  case class Input(
      fromTxid: TransactionId,
      fromOutputIndex: Int,
      index: Int,
      value: BigDecimal,
      addresses: List[Address]
  ) {

    def address: Option[Address] = addresses.headOption
  }

  object Input {

    def apply(
        fromTxid: TransactionId,
        fromOutputIndex: Int,
        index: Int,
        value: BigDecimal,
        address: Address
    ): Input = {

      Input(fromTxid, fromOutputIndex, index, value, List(address))
    }
  }

  case class Output(
      txid: TransactionId,
      index: Int,
      value: BigDecimal,
      addresses: List[Address],
      script: HexString
  ) {

    def address: Option[Address] = addresses.headOption
  }

  object Output {

    def apply(
        txid: TransactionId,
        index: Int,
        value: BigDecimal,
        address: Address,
        script: HexString
    ): Output = {

      new Output(txid, index, value, List(address), script)
    }
  }

  case class HasIO(
      transaction: Transaction,
      inputs: List[Transaction.Input],
      outputs: List[Transaction.Output]
  ) {

    require(
      outputs.forall(_.txid == transaction.id),
      "There are outputs that having a different txid"
    )

    def id: TransactionId = transaction.id
    def time: Long = transaction.time
    def blockhash: Blockhash = transaction.blockhash
    def size: Size = transaction.size
    def sent: BigDecimal = inputs.map(_.value).sum
    def received: BigDecimal = outputs.map(_.value).sum
  }

  /** Transform a rpc transaction to a persisted transaction.
    *
    * As the TPoS contracts aren't stored in the persisted transaction, they are returned as possible contracts
    */
  def fromRPC(
      tx: rpc.Transaction[TransactionVIN.HasValues]
  ): (HasIO, Boolean) = {
    val inputs = tx.vin.zipWithIndex
      .map { case (vin, index) =>
        Transaction.Input(
          vin.txid,
          vin.voutIndex,
          index,
          vin.value,
          vin.addresses
        )
      }
      .filter(_.value > 0)

    val outputs = tx.vout.flatMap { vout =>
      for {
        _ <- vout.addresses if vout.value > 0
        script <- vout.scriptPubKey.map(_.hex)
      } yield Output(
        tx.id,
        vout.n,
        vout.value,
        vout.addresses.getOrElse(List.empty),
        script
      )
    }

    val transaction = Transaction(
      id = tx.id,
      blockhash = tx.blockhash,
      time = tx.time,
      size = tx.size
    )

    (HasIO(transaction, inputs, outputs), seemsTPoSContract(tx))
  }

  /** A transaction can have at most one contract
    */
  private def seemsTPoSContract(
      tx: rpc.Transaction[rpc.TransactionVIN.HasValues]
  ): Boolean = {
    val collateralMaybe = tx.vout.find(_.value == 1)
    val zeroAmount = tx.vout.find(_.value == 0)
    val hasOpReturn = zeroAmount.exists(t => t.scriptPubKey.exists(_.asm.startsWith("OP_RETURN")))

    collateralMaybe.isDefined && zeroAmount.isDefined && hasOpReturn
  }
}
