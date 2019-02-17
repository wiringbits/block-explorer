package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.TransactionId
import com.xsn.explorer.models.values.Address

case class AddressTransactionDetails(
    address: Address,
    txid: TransactionId,
    time: Long,
    received: BigDecimal = 0,
    sent: BigDecimal = 0)
