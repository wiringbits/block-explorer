package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.BlockExtractionMethod
import com.xsn.explorer.models.values._

case class Block(
    hash: Blockhash,
    previousBlockhash: Option[Blockhash],
    nextBlockhash: Option[Blockhash],
    tposContract: Option[TransactionId],
    merkleRoot: Blockhash,
    size: Size,
    height: Height,
    version: Int,
    time: Long,
    medianTime: Long,
    nonce: Long,
    bits: String,
    chainwork: String,
    difficulty: BigDecimal,
    extractionMethod: BlockExtractionMethod) {

  def withTransactions(transactions: List[Transaction.HasIO]): Block.HasTransactions = {
    Block.HasTransactions(this, transactions)
  }
}

object Block {

  case class HasTransactions(block: Block, transactions: List[Transaction.HasIO]) {
    require(
      transactions.forall(_.blockhash == block.hash),
      s"The transaction = ${transactions.find(_.blockhash != block.hash).get.id} doesn't belong to the block = ${block.hash}"
    )

    def hash: Blockhash = block.hash
    def height: Height = block.height
    def previousBlockhash: Option[Blockhash] = block.previousBlockhash
    def asTip: HasTransactions = HasTransactions(block.copy(nextBlockhash = None), transactions)
  }
}
