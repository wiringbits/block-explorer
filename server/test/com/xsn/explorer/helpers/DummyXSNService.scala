package com.xsn.explorer.helpers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.{
  Block,
  Masternode,
  Transaction,
  TransactionVIN
}
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.XSNService
import play.api.libs.json.JsValue

class DummyXSNService extends XSNService {

  override def genesisBlockhash: Blockhash =
    Blockhash
      .from("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
      .get
  override def getTransaction(
      txid: TransactionId
  ): FutureApplicationResult[Transaction[TransactionVIN]] = ???
  override def getRawTransaction(
      txid: TransactionId
  ): FutureApplicationResult[JsValue] = ???
  override def getBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[rpc.Block.Canonical] = ???
  override def getFullBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[Block.HasTransactions[TransactionVIN]] = ???
  override def getRawBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[JsValue] = ???
  override def getBlockhash(
      height: Height
  ): FutureApplicationResult[Blockhash] = ???
  override def getLatestBlock(): FutureApplicationResult[rpc.Block.Canonical] =
    ???
  override def getServerStatistics()
      : FutureApplicationResult[rpc.ServerStatistics] = ???
  override def getMasternodeCount(): FutureApplicationResult[Int] = ???
  override def getDifficulty(): FutureApplicationResult[BigDecimal] = ???
  override def getMasternodes(): FutureApplicationResult[List[rpc.Masternode]] =
    ???
  override def getMasternode(
      ipAddress: IPAddress
  ): FutureApplicationResult[Masternode] = ???
  override def getMerchantnodes()
      : FutureApplicationResult[List[rpc.Merchantnode]] = ???
  override def getUnspentOutputs(
      address: Address
  ): FutureApplicationResult[JsValue] = ???
  override def sendRawTransaction(
      hex: HexString
  ): FutureApplicationResult[String] = ???
  override def isTPoSContract(
      txid: TransactionId
  ): FutureApplicationResult[Boolean] = ???
  override def estimateSmartFee(
      confirmationsTarget: Int
  ): FutureApplicationResult[JsValue] = ???
  override def getTxOut(
      txid: TransactionId,
      index: Int,
      includeMempool: Boolean
  ): FutureApplicationResult[JsValue] =
    ???
  override def getFullRawBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[JsValue] = ???

  override def getHexEncodedBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[String] = ???

  override def encodeTPOSContract(
      tposAddress: Address,
      merchantAddress: Address,
      commission: Int,
      signature: String
  ): FutureApplicationResult[String] = ???

  override def getTPoSContractDetails(
      transactionId: TransactionId
  ): FutureApplicationResult[TPoSContract.Details] = ???
}
