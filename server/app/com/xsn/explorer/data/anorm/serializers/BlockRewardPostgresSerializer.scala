package com.xsn.explorer.data.anorm.serializers

import com.xsn.explorer.models.{
  BlockReward,
  BlockRewards,
  PoSBlockRewards,
  PoWBlockRewards,
  RewardType,
  TPoSBlockRewards
}

object BlockRewardPostgresSerializer {
  case class Reward(
      blockReward: BlockReward,
      rewardType: RewardType,
      stake: Option[Stake]
  )
  case class Stake(stakedAmount: BigDecimal, stakedTime: Long)

  def serialize(reward: BlockRewards): List[Reward] = {
    reward match {
      case r: PoWBlockRewards =>
        List(Reward(r.reward, RewardType.PoW, None))
      case r: PoSBlockRewards =>
        val rewards = List(
          Reward(
            r.coinstake,
            RewardType.PoS,
            Some(Stake(r.stakedAmount, r.stakedDuration))
          )
        )

        rewards ++ r.masternode.map(Reward(_, RewardType.Masternode, None))
      case r: TPoSBlockRewards =>
        val rewards = List(
          Reward(
            r.owner,
            RewardType.TPoSOwner,
            Some(Stake(r.stakedAmount, r.stakedDuration))
          ),
          Reward(r.merchant, RewardType.TPoSMerchant, None)
        )

        rewards ++ r.masternode.map(Reward(_, RewardType.Masternode, None))
    }
  }

  def deserialize(rewards: List[Reward]): Option[BlockRewards] = {
    rewards match {
      case Reward(r, RewardType.PoW, _) :: Nil =>
        Some(PoWBlockRewards(r))
      case Reward(
            r,
            RewardType.PoS,
            Some(Stake(stakedAmount, stakedTime))
          ) :: Nil =>
        Some(PoSBlockRewards(r, None, stakedAmount, stakedTime))
      case Reward(r, RewardType.PoS, Some(Stake(stakedAmount, stakedTime)))
          :: Reward(masternode, RewardType.Masternode, _) :: Nil =>
        Some(PoSBlockRewards(r, Some(masternode), stakedAmount, stakedTime))
      case Reward(
            owner,
            RewardType.TPoSOwner,
            Some(Stake(stakedAmount, stakedTime))
          )
          :: Reward(merchant, RewardType.TPoSMerchant, _) :: Nil =>
        Some(TPoSBlockRewards(owner, merchant, None, stakedAmount, stakedTime))
      case Reward(masternode, RewardType.Masternode, _)
          :: Reward(
            owner,
            RewardType.TPoSOwner,
            Some(Stake(stakedAmount, stakedTime))
          )
          :: Reward(merchant, RewardType.TPoSMerchant, _) :: Nil =>
        Some(
          TPoSBlockRewards(
            owner,
            merchant,
            Some(masternode),
            stakedAmount,
            stakedTime
          )
        )
      case Nil =>
        None
      case _ =>
        throw new RuntimeException("Unknown reward type")
    }
  }
}
