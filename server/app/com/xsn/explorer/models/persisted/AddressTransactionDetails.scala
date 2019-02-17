package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.values.{Address, TransactionId}

case class AddressTransactionDetails(
    address: Address,
    txid: TransactionId,
    time: Long,
    received: BigDecimal = 0,
    sent: BigDecimal = 0)
