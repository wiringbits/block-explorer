package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.models.{
  BlockReward,
  BlockRewards,
  PoSBlockRewards,
  PoWBlockRewards,
  RewardType,
  TPoSBlockRewards
}
import com.xsn.explorer.models.values.Blockhash
import com.xsn.explorer.data.anorm.parsers.BlockRewardParsers.parseBlockReward

class BlockRewardPostgresDAO {

  def upsert(blockhash: Blockhash, reward: BlockRewards)(implicit conn: Connection): Unit = {
    getRewards(reward).foreach(r => upsert(blockhash, r._1, r._2))
  }

  def getBy(blockhash: Blockhash)(implicit conn: Connection): Option[BlockRewards] = {
    val rewards = getRewards(blockhash)
    rewards match {
      case (r, RewardType.PoW) :: Nil =>
        Some(PoWBlockRewards(r))
      case (r, RewardType.PoS) :: Nil =>
        Some(PoSBlockRewards(r, None))
      case (r, RewardType.PoS) :: (masternode, RewardType.Masternode) :: Nil =>
        Some(PoSBlockRewards(r, Some(masternode)))
      case (owner, RewardType.TPoSOwner) :: (merchant, RewardType.TPoSMerchant) :: Nil =>
        Some(TPoSBlockRewards(owner, merchant, None))
      case (masternode, RewardType.Masternode) :: (owner, RewardType.TPoSOwner) :: (merchant, RewardType.TPoSMerchant) :: Nil =>
        Some(TPoSBlockRewards(owner, merchant, Some(masternode)))
      case Nil => None
      case _ =>
        throw new RuntimeException("Unknown reward type")
    }
  }

  def deleteBy(blockhash: Blockhash)(implicit conn: Connection): List[(BlockReward, RewardType)] = {
    SQL(
      """
        |DELETE FROM block_rewards
        |WHERE blockhash = {blockhash}
        |RETURNING address, value, type
      """.stripMargin
    ).on(
        'blockhash -> blockhash.toBytesBE.toArray
      )
      .as(parseBlockReward.*)
  }

  private def upsert(blockhash: Blockhash, reward: BlockReward, rewardType: RewardType)(
      implicit conn: Connection
  ): Unit = {
    SQL(
      """
        |INSERT INTO block_rewards(blockhash, value, address, type)
        |VALUES({blockhash}, {value}, {address}, {type}::REWARD_TYPE)
        |ON CONFLICT (blockhash, type) DO UPDATE
        |SET blockhash = EXCLUDED.blockhash,
        |    value = EXCLUDED.value,
        |    address = EXCLUDED.address,
        |    type = EXCLUDED.type
      """.stripMargin
    ).on(
        'blockhash -> blockhash.toBytesBE.toArray,
        'value -> reward.value,
        'address -> reward.address.string,
        'type -> rewardType.entryName
      )
      .execute()
  }

  private def getRewards(blockhash: Blockhash)(
      implicit conn: Connection
  ): List[(BlockReward, RewardType)] = {
    SQL(
      """
        |SELECT address, value, type
        |FROM block_rewards
        |WHERE blockhash = {blockhash}
        |ORDER BY type
    """.stripMargin
    ).on(
        "blockhash" -> blockhash.toBytesBE.toArray
      )
      .as(parseBlockReward.*)
  }

  private def getRewards(reward: BlockRewards): List[(BlockReward, RewardType)] = {
    reward match {
      case r: PoWBlockRewards =>
        List((r.reward, RewardType.PoW))
      case r: PoSBlockRewards =>
        val rewards = List((r.coinstake, RewardType.PoS))

        rewards ++ r.masternode.map((_, RewardType.Masternode))
      case r: TPoSBlockRewards =>
        val rewards = List((r.owner, RewardType.TPoSOwner), (r.merchant, RewardType.TPoSMerchant))

        rewards ++ r.masternode.map((_, RewardType.Masternode))
    }
  }
}
