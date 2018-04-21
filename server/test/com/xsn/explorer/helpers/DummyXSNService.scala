package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.models._
import com.xsn.explorer.services.XSNService

class DummyXSNService extends XSNService {

  override def getTransaction(txid: TransactionId): FutureApplicationResult[rpc.Transaction] = ???
  override def getAddressBalance(address: Address): FutureApplicationResult[rpc.AddressBalance] = ???
  override def getTransactions(address: Address): FutureApplicationResult[List[TransactionId]] = ???
  override def getBlock(blockhash: Blockhash): FutureApplicationResult[rpc.Block] = ???
  override def getLatestBlock(): FutureApplicationResult[rpc.Block] = ???
  override def getServerStatistics(): FutureApplicationResult[rpc.ServerStatistics] = ???
  override def getMasternodeCount(): FutureApplicationResult[Int] = ???
  override def getMasternodes(): FutureApplicationResult[List[rpc.Masternode]] = ???
}
