package com.xsn.explorer.helpers

import com.xsn.explorer.models.values.{TransactionId, _}

object DataHelper {

  def createAddress(string: String) = Address.from(string).get

  def createBlockhash(string: String) = Blockhash.from(string).get

  def createTransactionId(string: String) = TransactionId.from(string).get
}
