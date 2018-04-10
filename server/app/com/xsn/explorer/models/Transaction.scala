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
}
