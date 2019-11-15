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
  type Reward = (BlockReward, RewardType, Option[BigDecimal], Option[Long])

  def upsert(blockhash: Blockhash, reward: BlockRewards)(implicit conn: Connection): Unit = {
    getRewards(reward).foreach(r => upsert(blockhash, r._1, r._2, r._3, r._4))
  }

  def getBy(blockhash: Blockhash)(implicit conn: Connection): Option[BlockRewards] = {
    val rewards = getRewards(blockhash)
    rewards match {
      case (r, RewardType.PoW, _, _) :: Nil =>
        Some(PoWBlockRewards(r))
      case (r, RewardType.PoS, Some(stakedAmount), Some(stakedTime)) :: Nil =>
        Some(PoSBlockRewards(r, None, stakedAmount, stakedTime))
      case (r, RewardType.PoS, Some(stakedAmount), Some(stakedTime))
            :: (masternode, RewardType.Masternode, _, _) :: Nil =>
        Some(PoSBlockRewards(r, Some(masternode), stakedAmount, stakedTime))
      case (owner, RewardType.TPoSOwner, Some(stakedAmount), Some(stakedTime))
            :: (merchant, RewardType.TPoSMerchant, _, _) :: Nil =>
        Some(TPoSBlockRewards(owner, merchant, None, stakedAmount, stakedTime))
      case (masternode, RewardType.Masternode, _, _)
            :: (owner, RewardType.TPoSOwner, Some(stakedAmount), Some(stakedTime))
            :: (merchant, RewardType.TPoSMerchant, _, _) :: Nil =>
        Some(TPoSBlockRewards(owner, merchant, Some(masternode), stakedAmount, stakedTime))
      case Nil =>
        None
      case _ =>
        throw new RuntimeException("Unknown reward type")
    }
  }

  def deleteBy(blockhash: Blockhash)(implicit conn: Connection): List[Reward] = {
    SQL(
      """
        |DELETE FROM block_rewards
        |WHERE blockhash = {blockhash}
        |RETURNING address, value, type, staked_amount, staked_time
      """.stripMargin
    ).on(
        'blockhash -> blockhash.toBytesBE.toArray
      )
      .as(parseBlockReward.*)
  }

  private def upsert(
      blockhash: Blockhash,
      reward: BlockReward,
      rewardType: RewardType,
      stakedAmount: Option[BigDecimal],
      stakedTime: Option[Long]
  )(
      implicit conn: Connection
  ): Unit = {
    SQL(
      """
        |INSERT INTO block_rewards(blockhash, value, address, type, staked_amount, staked_time)
        |VALUES({blockhash}, {value}, {address}, {type}::REWARD_TYPE, {staked_amount}, {staked_time})
        |ON CONFLICT (blockhash, type) DO UPDATE
        |SET blockhash = EXCLUDED.blockhash,
        |    value = EXCLUDED.value,
        |    address = EXCLUDED.address,
        |    type = EXCLUDED.type,
        |    staked_amount = EXCLUDED.staked_amount,
        |    staked_time = EXCLUDED.staked_time
      """.stripMargin
    ).on(
        'blockhash -> blockhash.toBytesBE.toArray,
        'value -> reward.value,
        'address -> reward.address.string,
        'type -> rewardType.entryName,
        'staked_amount -> stakedAmount,
        'staked_time -> stakedTime
      )
      .execute()
  }

  private def getRewards(blockhash: Blockhash)(implicit conn: Connection): List[Reward] = {
    SQL(
      """
        |SELECT address, value, type, staked_amount, staked_time
        |FROM block_rewards
        |WHERE blockhash = {blockhash}
        |ORDER BY type
    """.stripMargin
    ).on(
        "blockhash" -> blockhash.toBytesBE.toArray
      )
      .as(parseBlockReward.*)
  }

  private def getRewards(reward: BlockRewards): List[Reward] = {
    reward match {
      case r: PoWBlockRewards =>
        List((r.reward, RewardType.PoW, None, None))
      case r: PoSBlockRewards =>
        val rewards = List((r.coinstake, RewardType.PoS, Some(r.stakedAmount), Some(r.stakedDuration)))

        rewards ++ r.masternode.map((_, RewardType.Masternode, None, None))
      case r: TPoSBlockRewards =>
        val rewards = List(
          (r.owner, RewardType.TPoSOwner, Some(r.stakedAmount), Some(r.stakedDuration)),
          (r.merchant, RewardType.TPoSMerchant, None, None)
        )

        rewards ++ r.masternode.map((_, RewardType.Masternode, None, None))
    }
  }
}
