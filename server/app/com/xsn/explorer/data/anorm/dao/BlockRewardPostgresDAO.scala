package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.models.values.Blockhash
import com.xsn.explorer.data.anorm.parsers.BlockRewardParsers.parseBlockReward
import com.xsn.explorer.data.anorm.serializers.BlockRewardPostgresSerializer.Reward

class BlockRewardPostgresDAO {

  def upsert(blockhash: Blockhash, reward: Reward)(implicit
      conn: Connection
  ): Unit = {
    val _ = SQL(
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
      Symbol("blockhash") -> blockhash.toBytesBE.toArray,
      Symbol("value") -> reward.blockReward.value,
      Symbol("address") -> reward.blockReward.address.string,
      Symbol("type") -> reward.rewardType.entryName,
      Symbol("staked_amount") -> reward.stake.map(_.stakedAmount),
      Symbol("staked_time") -> reward.stake.map(_.stakedTime)
    ).execute()
  }

  def deleteBy(
      blockhash: Blockhash
  )(implicit conn: Connection): List[Reward] = {
    SQL(
      """
        |DELETE FROM block_rewards
        |WHERE blockhash = {blockhash}
        |RETURNING address, value, type, staked_amount, staked_time
      """.stripMargin
    ).on(
      Symbol("blockhash") -> blockhash.toBytesBE.toArray
    ).as(parseBlockReward.*)
  }

  def getBy(blockhash: Blockhash)(implicit conn: Connection): List[Reward] = {
    SQL(
      """
        |SELECT address, value, type, staked_amount, staked_time
        |FROM block_rewards
        |WHERE blockhash = {blockhash}
        |ORDER BY type
    """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray
    ).as(parseBlockReward.*)
  }
}
