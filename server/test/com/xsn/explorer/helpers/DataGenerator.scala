package com.xsn.explorer.helpers

import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.values.{TransactionId, _}

trait DataGenerator {

  import scala.util.Random._

  val hexCharacters = ((0 to 9) ++ ('a' to 'f')).mkString("")

  def randomItem[T](items: List[T]): T = {
    val index = scala.util.Random.nextInt(items.size)
    items(index)
  }

  def randomItems[T](items: List[T], n: Int): List[T] = {
    (0 until n).map(_ => randomItem(items)).toList
  }

  def randomHexString(length: Int = 8): HexString = {
    val string = randomItems(hexCharacters.toList, length)
    HexString.from(string.mkString("")).get
  }

  def randomBlockhash: Blockhash = {
    val hex = randomHexString(Blockhash.Length)
    Blockhash.from(hex.string).get
  }

  def randomTransactionId: TransactionId = {
    val hex = randomHexString(TransactionId.Length)
    TransactionId.from(hex.string).get
  }

  def randomAddress: Address = {
    val hex = randomHexString(34)
    Address.from(hex.string).get
  }

  def randomBlock(
      blockhash: Blockhash = randomBlockhash,
      previousBlockhash: Option[Blockhash] = None,
      nextBlockhash: Option[Blockhash] = None
  ): Block.Canonical = {

    Block.Canonical(
      hash = blockhash,
      previousBlockhash = previousBlockhash,
      nextBlockhash = nextBlockhash,
      merkleRoot = randomBlockhash,
      transactions = List.empty,
      confirmations = Confirmations(0),
      size = Size(10),
      height = Height(nextInt(10000)),
      version = 0,
      time = 0,
      medianTime = 0,
      nonce = 0,
      bits = "abcdef",
      chainwork = "abcdef",
      difficulty = 12.2,
      tposContract = None
    )
  }

  def randomOutput(txid: TransactionId = randomTransactionId, index: Int = nextInt(100)): Transaction.Output = {
    Transaction.Output(
      txid = txid,
      index = index,
      value = nextInt(1000000),
      address = randomAddress,
      script = randomHexString(8)
    )
  }

  def randomOutputs(
      howMany: Int = nextInt(5) + 1,
      txid: TransactionId = randomTransactionId
  ): List[Transaction.Output] = {
    (0 until howMany).map { index =>
      randomOutput(txid, index)
    }.toList
  }

  def randomInput(utxos: List[Transaction.Output]): Transaction.Input = {
    val utxo = randomItem(utxos)
    Transaction.Input(
      fromTxid = utxo.txid,
      fromOutputIndex = utxo.index,
      index = scala.util.Random.nextInt(100),
      value = utxo.value,
      address = randomAddress
    )
  }

  def randomInput(utxo: Transaction.Output): Transaction.Input = {
    Transaction.Input(
      fromTxid = utxo.txid,
      fromOutputIndex = utxo.index,
      index = scala.util.Random.nextInt(100),
      value = utxo.value,
      address = randomAddress
    )
  }

  def randomInputs(utxos: List[Transaction.Output]): List[Transaction.Input] = utxos match {
    case Nil => List.empty
    case _ =>
      randomItems(utxos, scala.util.Random.nextInt(utxos.size))
        .map(randomInput)
  }

  /**
   * Generate a random transaction spending the given utxos
   */
  def randomTransaction(
      blockhash: Blockhash,
      id: TransactionId = randomTransactionId,
      utxos: List[Transaction.Output]
  ): Transaction.HasIO = {

    Transaction.HasIO(
      Transaction(id = id, blockhash = blockhash, time = java.lang.System.currentTimeMillis(), Size(1000)),
      createInputs(utxos),
      randomOutputs().map(_.copy(txid = id))
    )
  }

  def createInputs(outputs: List[Transaction.Output]): List[Transaction.Input] = {
    outputs.zipWithIndex.map {
      case (output, index) =>
        Transaction.Input(
          fromTxid = output.txid,
          fromOutputIndex = output.index,
          index = index,
          value = output.value,
          addresses = output.addresses,
          Some(output.script)
        )
    }
  }

  def randomTPoSContract(
      txid: TransactionId = randomTransactionId,
      index: Int = scala.util.Random.nextInt(100)
  ): TPoSContract = {
    val state = randomItem(TPoSContract.State.values.toList)
    val commission = TPoSContract.Commission.from(scala.util.Random.nextInt(50) + 1).get
    val details = TPoSContract.Details(randomAddress, randomAddress, commission)
    TPoSContract(TPoSContract.Id(txid, index), details = details, time = System.currentTimeMillis(), state)
  }
}

object DataGenerator extends DataGenerator
