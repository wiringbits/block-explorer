package com.xsn.explorer.data.serializers

import com.xsn.explorer.data.anorm.serializers.BlockRewardPostgresSerializer
import com.xsn.explorer.data.anorm.serializers.BlockRewardPostgresSerializer.{Reward, Stake}
import com.xsn.explorer.helpers.DataGenerator
import com.xsn.explorer.models.{BlockReward, PoSBlockRewards, PoWBlockRewards, RewardType, TPoSBlockRewards}
import org.scalatest.{MustMatchers, WordSpec}

@com.github.ghik.silencer.silent
class BlockRewardPostgresSerializerSpec extends WordSpec with MustMatchers {
  "serialize" should {
    "serialize PoW block" in {
      val blockReward = PoWBlockRewards(BlockReward(DataGenerator.randomAddress, 100))
      val result = BlockRewardPostgresSerializer.serialize(blockReward)

      result.head.blockReward.address mustEqual blockReward.reward.address
      result.head.blockReward.value mustEqual blockReward.reward.value
      result.head.rewardType mustEqual RewardType.PoW
      result.head.stake mustEqual None
    }

    "serialize PoS block with masternode" in {
      val blockReward =
        PoSBlockRewards(
          BlockReward(DataGenerator.randomAddress, 100),
          Some(BlockReward(DataGenerator.randomAddress, 50)),
          1000,
          123
        )
      val result = BlockRewardPostgresSerializer.serialize(blockReward)

      result.length mustEqual 2

      result(0).blockReward.address mustEqual blockReward.coinstake.address
      result(0).blockReward.value mustEqual blockReward.coinstake.value
      result(0).rewardType mustEqual RewardType.PoS
      result(0).stake.get.stakedAmount mustEqual blockReward.stakedAmount
      result(0).stake.get.stakedTime mustEqual blockReward.stakedDuration

      result(1).blockReward.address mustEqual blockReward.masternode.get.address
      result(1).blockReward.value mustEqual blockReward.masternode.get.value
      result(1).rewardType mustEqual RewardType.Masternode
      result(1).stake mustEqual None
    }

    "serialize PoS block without masternode" in {
      val blockReward = PoSBlockRewards(BlockReward(DataGenerator.randomAddress, 100), None, 1000, 123)
      val result = BlockRewardPostgresSerializer.serialize(blockReward)

      result.length mustEqual 1

      result(0).blockReward.address mustEqual blockReward.coinstake.address
      result(0).blockReward.value mustEqual blockReward.coinstake.value
      result(0).rewardType mustEqual RewardType.PoS
      result(0).stake.get.stakedAmount mustEqual blockReward.stakedAmount
      result(0).stake.get.stakedTime mustEqual blockReward.stakedDuration
    }

    "serialize TPoS block with masternode" in {
      val blockReward =
        TPoSBlockRewards(
          BlockReward(DataGenerator.randomAddress, 100),
          BlockReward(DataGenerator.randomAddress, 200),
          Some(BlockReward(DataGenerator.randomAddress, 50)),
          1000,
          123
        )
      val result = BlockRewardPostgresSerializer.serialize(blockReward)

      result.length mustEqual 3

      result(0).blockReward.address mustEqual blockReward.owner.address
      result(0).blockReward.value mustEqual blockReward.owner.value
      result(0).rewardType mustEqual RewardType.TPoSOwner
      result(0).stake.get.stakedAmount mustEqual blockReward.stakedAmount
      result(0).stake.get.stakedTime mustEqual blockReward.stakedDuration

      result(1).blockReward.address mustEqual blockReward.merchant.address
      result(1).blockReward.value mustEqual blockReward.merchant.value
      result(1).rewardType mustEqual RewardType.TPoSMerchant
      result(1).stake mustEqual None

      result(2).blockReward.address mustEqual blockReward.masternode.get.address
      result(2).blockReward.value mustEqual blockReward.masternode.get.value
      result(2).rewardType mustEqual RewardType.Masternode
      result(2).stake mustEqual None
    }

    "serialize TPoS block without masternode" in {
      val blockReward =
        TPoSBlockRewards(
          BlockReward(DataGenerator.randomAddress, 100),
          BlockReward(DataGenerator.randomAddress, 200),
          None,
          1000,
          123
        )
      val result = BlockRewardPostgresSerializer.serialize(blockReward)

      result.length mustEqual 2

      result(0).blockReward.address mustEqual blockReward.owner.address
      result(0).blockReward.value mustEqual blockReward.owner.value
      result(0).rewardType mustEqual RewardType.TPoSOwner
      result(0).stake.get.stakedAmount mustEqual blockReward.stakedAmount
      result(0).stake.get.stakedTime mustEqual blockReward.stakedDuration

      result(1).blockReward.address mustEqual blockReward.merchant.address
      result(1).blockReward.value mustEqual blockReward.merchant.value
      result(1).rewardType mustEqual RewardType.TPoSMerchant
      result(1).stake mustEqual None
    }
  }

  "deserialize" should {
    "deserialize PoW block" in {
      val rewards = List(Reward(BlockReward(DataGenerator.randomAddress, 100), RewardType.PoW, None))
      val result = BlockRewardPostgresSerializer.deserialize(rewards)

      result match {
        case Some(r: PoWBlockRewards) =>
          r.reward.value mustEqual rewards.head.blockReward.value
          r.reward.address mustEqual rewards.head.blockReward.address
        case _ => fail
      }
    }

    "deserialize PoS block with masternode" in {
      val rewards = List(
        Reward(BlockReward(DataGenerator.randomAddress, 100), RewardType.PoS, Some(Stake(1000, 123))),
        Reward(BlockReward(DataGenerator.randomAddress, 50), RewardType.Masternode, None)
      )
      val result = BlockRewardPostgresSerializer.deserialize(rewards)

      result match {
        case Some(r: PoSBlockRewards) =>
          r.coinstake.value mustEqual rewards(0).blockReward.value
          r.coinstake.address mustEqual rewards(0).blockReward.address
          r.stakedAmount mustEqual rewards(0).stake.get.stakedAmount
          r.stakedDuration mustEqual rewards(0).stake.get.stakedTime
          r.masternode.get.value mustEqual rewards(1).blockReward.value
          r.masternode.get.address mustEqual rewards(1).blockReward.address
        case _ => fail
      }
    }

    "deserialize PoS block without masternode" in {
      val rewards = List(Reward(BlockReward(DataGenerator.randomAddress, 100), RewardType.PoS, Some(Stake(1000, 123))))
      val result = BlockRewardPostgresSerializer.deserialize(rewards)

      result match {
        case Some(r: PoSBlockRewards) =>
          r.coinstake.value mustEqual rewards.head.blockReward.value
          r.coinstake.address mustEqual rewards.head.blockReward.address
          r.stakedAmount mustEqual rewards.head.stake.get.stakedAmount
          r.stakedDuration mustEqual rewards.head.stake.get.stakedTime
          r.masternode mustEqual None
        case _ => fail
      }
    }

    "deserialize TPoS block with masternode" in {
      val rewards = List(
        Reward(BlockReward(DataGenerator.randomAddress, 50), RewardType.Masternode, None),
        Reward(BlockReward(DataGenerator.randomAddress, 100), RewardType.TPoSOwner, Some(Stake(1000, 123))),
        Reward(BlockReward(DataGenerator.randomAddress, 200), RewardType.TPoSMerchant, None)
      )
      val result = BlockRewardPostgresSerializer.deserialize(rewards)

      result match {
        case Some(r: TPoSBlockRewards) =>
          r.masternode.get.value mustEqual rewards(0).blockReward.value
          r.masternode.get.address mustEqual rewards(0).blockReward.address
          r.owner.value mustEqual rewards(1).blockReward.value
          r.owner.address mustEqual rewards(1).blockReward.address
          r.stakedAmount mustEqual rewards(1).stake.get.stakedAmount
          r.stakedDuration mustEqual rewards(1).stake.get.stakedTime
          r.merchant.value mustEqual rewards(2).blockReward.value
          r.merchant.address mustEqual rewards(2).blockReward.address
        case _ => fail
      }
    }

    "deserialize TPoS block without masternode" in {
      val rewards = List(
        Reward(BlockReward(DataGenerator.randomAddress, 100), RewardType.TPoSOwner, Some(Stake(1000, 123))),
        Reward(BlockReward(DataGenerator.randomAddress, 200), RewardType.TPoSMerchant, None)
      )
      val result = BlockRewardPostgresSerializer.deserialize(rewards)

      result match {
        case Some(r: TPoSBlockRewards) =>
          r.masternode mustEqual None
          r.owner.value mustEqual rewards(0).blockReward.value
          r.owner.address mustEqual rewards(0).blockReward.address
          r.stakedAmount mustEqual rewards(0).stake.get.stakedAmount
          r.stakedDuration mustEqual rewards(0).stake.get.stakedTime
          r.merchant.value mustEqual rewards(1).blockReward.value
          r.merchant.address mustEqual rewards(1).blockReward.address
        case _ => fail
      }
    }

    "deserialize empty list" in {
      val result = BlockRewardPostgresSerializer.deserialize(List.empty)

      result mustEqual None
    }

    "deserialize fail with an unknown reward structure" in {
      val rewards = List(Reward(BlockReward(DataGenerator.randomAddress, 200), RewardType.TPoSMerchant, None))
      an[RuntimeException] should be thrownBy {
        BlockRewardPostgresSerializer.deserialize(rewards)
      }
    }
  }
}
