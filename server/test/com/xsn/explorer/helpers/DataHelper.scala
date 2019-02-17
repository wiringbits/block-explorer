package com.xsn.explorer.helpers

import com.xsn.explorer.models.values._
import com.xsn.explorer.models.{Blockhash, TransactionId}

object DataHelper {

  def createAddress(string: String) = Address.from(string).get

  def createBlockhash(string: String) = Blockhash.from(string).get

  def createTransactionId(string: String) = TransactionId.from(string).get
}
