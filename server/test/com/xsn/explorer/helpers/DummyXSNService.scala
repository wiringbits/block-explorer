package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.{AddressBalance, Block, Transaction}
import com.xsn.explorer.services.XSNService

class DummyXSNService extends XSNService {

  override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction] = ???
  override def getAddressBalance(address: Address): FutureApplicationResult[AddressBalance] = ???
  override def getTransactions(address: Address): FutureApplicationResult[List[TransactionId]] = ???
  override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block] = ???
  override def getLatestBlock(): FutureApplicationResult[Block] = ???
}
