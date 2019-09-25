package com.xsn.explorer.data.anorm

import java.sql.Connection

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ApplicationError
import com.xsn.explorer.data.LedgerBlockingDataHandler
import com.xsn.explorer.data.anorm.dao._
import com.xsn.explorer.errors.{PostgresForeignKeyViolationError, PreviousBlockMissingError, RepeatedBlockHeightError}
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.{BlockRewards, PoSBlockRewards, PoWBlockRewards, TPoSBlockRewards, TPoSContract}
import com.xsn.explorer.models.persisted.{Balance, Block}
import com.xsn.explorer.models.values.Blockhash
import com.xsn.explorer.util.Extensions.ListOptionExt
import com.xsn.explorer.util.TransactionBalancesHelper
import javax.inject.Inject
import org.scalactic.Good
import org.slf4j.LoggerFactory
import play.api.db.Database

class LedgerPostgresDataHandler @Inject()(
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO,
    blockFilterPostgresDAO: BlockFilterPostgresDAO,
    transactionPostgresDAO: TransactionPostgresDAO,
    balancePostgresDAO: BalancePostgresDAO,
    aggregatedAmountPostgresDAO: AggregatedAmountPostgresDAO,
    blockRewardDAO: BlockRewardPostgresDAO
) extends LedgerBlockingDataHandler
    with AnormPostgresDataHandler {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Push a block into the database chain, note that even if the block is supposed
   * to have a next block, we remove the link because that block is not stored yet.
   */
  override def push(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): ApplicationResult[Unit] = {

    // the filter is computed outside the transaction to avoid unnecessary locking
    val filter = filterFactory()
    val result = withTransaction { implicit conn =>
      val result = for {
        _ <- upsertBlockCascade(block.asTip, filter, tposContracts, rewards)
      } yield ()

      result
        .map(Good(_))
        .getOrElse(throw new RuntimeException("Unable to push block"))
    }

    def fromError(e: ApplicationError) = e match {
      case PostgresForeignKeyViolationError("previous_blockhash", _) => PreviousBlockMissingError
      case PostgresForeignKeyViolationError("height", _) => RepeatedBlockHeightError
      case _ => e
    }

    result.badMap { _.map(fromError) }
  }

  /**
   * Pop a block from the database chain (if exists)
   */
  override def pop(): ApplicationResult[Block] = withTransaction { implicit conn =>
    val result = for {
      block <- blockPostgresDAO.getLatestBlock
      _ <- deleteBlockCascade(block)
    } yield block

    result
      .map(Good(_))
      .getOrElse(throw new RuntimeException("Unable to pop block"))
  }

  private def upsertBlockCascade(
      block: Block.HasTransactions,
      filter: GolombCodedSet,
      tposContracts: List[TPoSContract],
      rewards: Option[BlockRewards]
  )(implicit conn: Connection): Option[Unit] = {

    val result = for {
      // block
      _ <- deleteBlockCascade(block.block).orElse(Some(()))
      _ <- blockPostgresDAO.insert(block.block)
      _ = blockFilterPostgresDAO.insert(block.hash, filter)
      _ = rewards match {
        case Some(r: PoWBlockRewards) => storePoWReward(block.hash, r)
        case Some(r: PoSBlockRewards) => storePoSReward(block.hash, r)
        case Some(r: TPoSBlockRewards) => storeTPoSReward(block.hash, r)
        case _ => ()
      }

      // batch insert
      _ <- transactionPostgresDAO.insert(block.transactions, tposContracts)

      // balances
      balanceList = TransactionBalancesHelper.computeBalances(block.transactions)
      _ <- insertBalanceBatch(balanceList).toList.everything

      // compute aggregated amount
      delta = balanceList.map(_.available).sum
      _ = aggregatedAmountPostgresDAO.updateAvailableCoins(delta)
    } yield ()

    // link previous block (if possible)
    block.previousBlockhash.foreach { previousBlockhash =>
      blockPostgresDAO
        .setNextBlockhash(previousBlockhash, block.hash)
    }

    result
  }

  private def deleteBlockCascade(block: Block)(implicit conn: Connection): Option[Unit] = {
    // transactions
    val deletedTransactions = transactionPostgresDAO.deleteBy(block.hash)
    val _ = blockFilterPostgresDAO.delete(block.hash)
    for {
      // block
      _ <- blockPostgresDAO.delete(block.hash)

      // balances
      balanceList = TransactionBalancesHelper.computeBalances(deletedTransactions)
      _ <- balanceList
        .map { b =>
          b.copy(spent = -b.spent, received = -b.received)
        }
        .map { b =>
          balancePostgresDAO.upsert(b)
        }
        .toList
        .everything

      // compute aggregated amount
      delta = balanceList.map(_.available).sum
      _ = aggregatedAmountPostgresDAO.updateAvailableCoins(-delta)
    } yield ()
  }

  private def insertBalanceBatch(balanceList: Iterable[Balance])(implicit conn: Connection) = {
    balanceList.map { b =>
      balancePostgresDAO.upsert(b)
    }
  }

  private def storePoWReward(blockhash: Blockhash, powReward: PoWBlockRewards)(implicit conn: Connection) = {
    blockRewardDAO.upsert(blockhash, powReward.reward)
  }

  private def storePoSReward(blockhash: Blockhash, posReward: PoSBlockRewards)(implicit conn: Connection) = {
    blockRewardDAO.upsert(blockhash, posReward.coinstake)
  }

  private def storeTPoSReward(blockhash: Blockhash, tposReward: TPoSBlockRewards)(implicit conn: Connection) = {
    blockRewardDAO.upsert(blockhash, tposReward.owner)
    blockRewardDAO.upsert(blockhash, tposReward.merchant)
  }
}
