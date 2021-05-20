package com.xsn.explorer.helpers

import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values.Address

object LedgerHelper {

  private val list = List(
    "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34",
    "000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7",
    "000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8",
    "00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd",
    "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32",
    "00000267225f7dba55d9a3493740e7f0dde0f28a371d2c3b42e7676b5728d020",
    "0000017ee4121cd8ae22f7321041ccb953d53828824217a9dc61a1c857facf85"
  )

  val blockList = list.map(BlockLoader.getRPC(_))

  val fullBlockList = list.map { cur =>
    BlockLoader.getFullRPC(cur)
  }

  def getTransactions(block: rpc.Block.Canonical): List[Transaction.HasIO] = {
    block.transactions
      .map(_.string)
      .map(TransactionLoader.getWithValues(_))
      .map(Transaction.fromRPC)
      .map(_._1)
  }

  @com.github.ghik.silencer.silent
  def getPoWReward(block: rpc.Block.Canonical): PoWBlockRewards = {
    PoWBlockRewards(BlockReward(Address.from(list.head).get, 1000))
  }

  @com.github.ghik.silencer.silent
  def getPoSReward(block: rpc.Block.Canonical): PoSBlockRewards = {
    val reward = BlockReward(Address.from(list.head).get, 1000)
    val masternodeReward = BlockReward(Address.from(list.head).get, 250)

    PoSBlockRewards(reward, Some(masternodeReward), None, 10000, 120000)
  }

  @com.github.ghik.silencer.silent
  def getTPoSReward(block: rpc.Block.Canonical): TPoSBlockRewards = {
    val ownerReward = BlockReward(Address.from(list.head).get, 1000)
    val merchantReward = BlockReward(Address.from(list(1)).get, 100)
    val masternodeReward = BlockReward(Address.from(list.head).get, 250)

    TPoSBlockRewards(
      ownerReward,
      merchantReward,
      Some(masternodeReward),
      None,
      10000,
      120000
    )
  }
}
