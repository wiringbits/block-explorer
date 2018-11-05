package com.xsn.explorer.models

case class AddressTransactionDetails(
    address: Address,
    txid: TransactionId,
    time: Long,
    received: BigDecimal = 0,
    sent: BigDecimal = 0)

