package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.{Block, Transaction}
import com.xsn.explorer.services.XSNService

class DummyXSNService extends XSNService {

  override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction] = ???
  override def getAddressBalance(address: Address): FutureApplicationResult[AddressBalance] = ???
  override def getTransactionCount(address: Address): FutureApplicationResult[Int] = ???
  override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block] = ???
}
