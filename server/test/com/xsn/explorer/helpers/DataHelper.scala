package com.xsn.explorer.helpers

import com.xsn.explorer.models.rpc.{ScriptPubKey, TransactionVOUT}
import com.xsn.explorer.models.{Address, TransactionId}

object DataHelper {

  def createAddress(string: String) = Address.from(string).get

  def createTransactionId(string: String) = TransactionId.from(string).get

  def createTransactionVOUT(n: Int, value: BigDecimal, scriptPubKey: ScriptPubKey) = {
    TransactionVOUT(
      n = n,
      value = value,
      scriptPubKey = Some(scriptPubKey))
  }

  def createScriptPubKey(scriptType: String, address: Address) = {
    ScriptPubKey(scriptType, "", List(address))
  }

  def createScriptPubKey(scriptType: String, asm: String, address: Option[Address] = None) = {
    ScriptPubKey(scriptType, asm, address.toList)
  }
}
