package com.xsn.explorer.models

import com.xsn.explorer.models.values.{Address, Blockhash, HexString, Size, TransactionId}

case class LightWalletTransaction(
    id: TransactionId,
    blockhash: Blockhash,
    size: Size,
    time: Long,
    inputs: List[LightWalletTransaction.Input],
    outputs: List[LightWalletTransaction.Output])

object LightWalletTransaction {

  case class Input(txid: TransactionId, index: Int, value: BigDecimal)
  case class Output(index: Int, value: BigDecimal, addresses: List[Address], script: HexString)

}
