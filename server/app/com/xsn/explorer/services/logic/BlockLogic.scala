package com.xsn.explorer.services.logic

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.{BlockNotFoundError, BlockhashFormatError}
import com.xsn.explorer.models._
import org.scalactic.{Bad, Good, One, Or}

class BlockLogic {

  def getBlockhash(string: String): ApplicationResult[Blockhash] = {
    val maybe = Blockhash.from(string)
    Or.from(maybe, One(BlockhashFormatError))
  }

  /**
   * Get the coinstake transaction id for the given block.
   *
   * A PoS block contains at least 2 transactions:
   * - the 1st one is empty
   * - the 2nd one is the Coinstake transaction.
   */
  def getCoinstakeTransactionId(block: Block): ApplicationResult[TransactionId] = {
    val maybe = block.transactions.lift(1)

    Or.from(maybe, One(BlockNotFoundError))
  }

  /**
   * Computes the rewards for a PoS coinstake transaction.
   *
   * There should be a coinstake reward and possibly a master node reward.
   *
   * The rewards are computed based on the transaction output which is expected to
   * contain between 2 and 4 values:
   * - the 1st one is empty
   * - the 2nd one goes to the coinstake
   * - the 3rd one (if present) will go to the coinstake if the address matches, otherwise it goes to master node.
   * - the 4th one (if present) will go to the master node.
   *
   * While the previous format should be meet by the RPC server, we compute the rewards
   * based on coinstake address.
   *
   * Sometimes there could be rounding errors, for example, when the input is not exactly divisible by 2,
   * we return 0 in that case because the reward could be negative.
   */
  def getRewards(
      coinstakeTx: Transaction,
      coinstakeAddress: Address,
      coinstakeInput: BigDecimal): ApplicationResult[BlockRewards] = {

    // first vout is empty, useless
    val coinstakeVOUT = coinstakeTx.vout.drop(1)
    if (coinstakeVOUT.size >= 1 && coinstakeVOUT.size <= 3) {
      val value = coinstakeVOUT
          .filter(_.address contains coinstakeAddress)
          .map(_.value)
          .sum

      val coinstakeReward = BlockReward(
        coinstakeAddress,
        (value - coinstakeInput) max 0)

      val masternodeRewardOUT = coinstakeVOUT.filterNot(_.address contains coinstakeAddress)
      val masternodeAddressMaybe = masternodeRewardOUT.flatMap(_.address).headOption
      val masternodeRewardMaybe = masternodeAddressMaybe.map { masternodeAddress =>
        BlockReward(
          masternodeAddress,
          masternodeRewardOUT.filter(_.address contains masternodeAddress).map(_.value).sum
        )
      }

      Good(BlockRewards(coinstakeReward, masternodeRewardMaybe))
    } else {
      Bad(BlockNotFoundError).accumulating
    }
  }
}
