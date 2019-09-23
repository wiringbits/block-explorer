package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.models.BlockReward
import com.xsn.explorer.models.values.Blockhash

class BlockRewardPostgresDAO {

  def upsert(blockhash: Blockhash, reward: BlockReward)(implicit conn: Connection): Unit = {
    SQL(
      """
        |INSERT INTO block_rewards(blockhash, address, value)
        |VALUES({blockhash}, {address}, {value})
        |ON CONFLICT (blockhash, address) DO UPDATE
        |SET blockhash = EXCLUDED.blockhash,
        |    address = EXCLUDED.address,
        |    value = EXCLUDED.value
      """.stripMargin
    ).on(
        'blockhash -> blockhash.toBytesBE.toArray,
        'address -> reward.address.string,
        'value -> reward.value
      )
      .execute()
  }
}
