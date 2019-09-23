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

  def getBy(blockhash: Blockhash)(implicit conn: Connection): BlockRewards = {
    val rewards = getRewards(blockhash)
    rewards match {
      case (r: BlockReward, RewardType.PoW) :: Nil =>
        PoWBlockRewards(r)
      case (r: BlockReward, RewardType.PoS) :: Nil =>
        PoSBlockRewards(r, None)
      case (ownerReward: BlockReward, RewardType.TPoSOwner) :: (merchantReward: BlockReward, RewardType.TPoSMerchant) :: Nil =>
        TPoSBlockRewards(ownerReward, merchantReward, None)
      case (merchantReward: BlockReward, RewardType.TPoSMerchant) :: (ownerReward: BlockReward, RewardType.TPoSOwner) :: Nil =>
        TPoSBlockRewards(ownerReward, merchantReward, None)
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
    """.stripMargin
    ).on(
        "blockhash" -> blockhash.toBytesBE.toArray
      )
      .as(parseBlockReward.*)
  }

  private def getRewards(reward: BlockRewards): List[(BlockReward, RewardType)] = {
    reward match {
      case r: PoWBlockRewards => List((r.reward, RewardType.PoW))
      case r: PoSBlockRewards => List((r.coinstake, RewardType.PoS))
      case r: TPoSBlockRewards =>
        List(
          (r.owner, RewardType.TPoSOwner),
          (r.merchant, RewardType.TPoSMerchant)
        )
    }
  }
}
