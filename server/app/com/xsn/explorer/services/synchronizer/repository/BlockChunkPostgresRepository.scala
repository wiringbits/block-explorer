package com.xsn.explorer.services.synchronizer.repository

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.anorm.AnormPostgresDataHandler
import com.xsn.explorer.data.anorm.dao._
import com.xsn.explorer.data.anorm.serializers.BlockRewardPostgresSerializer
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.{BlockRewards, TPoSContract}
import com.xsn.explorer.models.persisted.{
  AddressTransactionDetails,
  Balance,
  Block,
  Transaction
}
import com.xsn.explorer.models.values.{Blockhash, TransactionId}
import com.xsn.explorer.services.synchronizer.BlockSynchronizationState
import com.xsn.explorer.util.Extensions.ListOptionExt
import com.xsn.explorer.util.TransactionBalancesHelper
import javax.inject.Inject
import org.scalactic.Good
import play.api.db.Database

class BlockChunkPostgresRepository @Inject() (
    override val database: Database,
    synchronizationProgressDAO: BlockSynchronizationProgressDAO,
    blockDAO: BlockPostgresDAO,
    blockRewardDAO: BlockRewardPostgresDAO,
    blockFilterDAO: BlockFilterPostgresDAO,
    transactionDAO: TransactionPostgresDAO,
    transactionInputDAO: TransactionInputPostgresDAO,
    transactionOutputDAO: TransactionOutputPostgresDAO,
    addressTransactionDetailsDAO: AddressTransactionDetailsPostgresDAO,
    balanceDAO: BalancePostgresDAO,
    aggregatedAmountDAO: AggregatedAmountPostgresDAO,
    tposContractDAO: TPoSContractDAO
) extends BlockChunkRepository.Blocking
    with AnormPostgresDataHandler {

  override def upsertSyncState(
      blockhash: Blockhash,
      state: BlockSynchronizationState
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      val _ = synchronizationProgressDAO.upsert(blockhash, state)
      Good(())
    }
  }

  override def findSyncState(
      blockhash: Blockhash
  ): ApplicationResult[Option[BlockSynchronizationState]] = {
    withConnection { implicit conn =>
      val result = synchronizationProgressDAO.find(blockhash)
      Good(result)
    }
  }

  override def findSyncingBlock(): ApplicationResult[Option[Blockhash]] =
    withConnection { implicit conn =>
      val result = synchronizationProgressDAO.findAny
      Good(result)
    }

  override def upsertBlock(block: Block): ApplicationResult[Unit] =
    withConnection { implicit conn =>
      blockDAO.upsert(block)
      Good(())
    }

  override def upsertFilter(
      blockhash: Blockhash,
      filter: GolombCodedSet
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      blockFilterDAO.upsert(blockhash, filter)
      Good(())
    }
  }

  override def setNextBlockhash(
      blockhash: Blockhash,
      nextBlockhash: Blockhash
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      blockDAO.setNextBlockhash(blockhash, nextBlockhash)
      Good(())
    }
  }

  override def upsertTransaction(
      index: Int,
      transaction: Transaction
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      val _ = transactionDAO
        .upsertTransaction(index, transaction)
        .getOrElse(
          throw new RuntimeException(
            s"Failed to insert transaction ${transaction.id}"
          )
        )
      Good(())
    }
  }

  override def upsertInput(
      txid: TransactionId,
      input: Transaction.Input
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      transactionInputDAO.upsert(txid, input)
      Good(())
    }
  }

  override def upsertOutput(
      output: Transaction.Output
  ): ApplicationResult[Unit] = withConnection { implicit conn =>
    transactionOutputDAO.upsert(output)
    Good(())
  }

  override def spendOutput(
      txid: TransactionId,
      index: Int,
      spentOn: TransactionId
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      transactionOutputDAO.spend(txid, index, spentOn)
      Good(())
    }
  }

  override def upsertAddressTransactionDetails(
      details: AddressTransactionDetails
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      addressTransactionDetailsDAO.upsert(details)
      Good(())
    }
  }

  override def closeContract(
      id: TPoSContract.Id,
      closedOn: TransactionId
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      tposContractDAO.close(id, closedOn)
      Good(())
    }
  }

  override def upsertContract(contract: TPoSContract): ApplicationResult[Unit] =
    withConnection { implicit conn =>
      tposContractDAO.upsert(contract)
      Good(())
    }

  /** Atomically do the following:
    * - Update the given balance list
    * - Based on the balance list, update the available coins
    * - Mark the block synchronization as complete (deleting the row)
    */
  override def atomicUpdateBalances(
      blockhash: Blockhash,
      balances: List[Balance]
  ): ApplicationResult[Unit] = {
    withTransaction { implicit conn =>
      // the balances
      for (balance <- balances) {
        balanceDAO
          .upsert(balance)
          .getOrElse(
            throw new RuntimeException(
              s"Failed to upsert balance for ${balance.address}"
            )
          )
      }

      // the available coins
      val delta = balances.map(_.available).sum
      aggregatedAmountDAO.updateAvailableCoins(delta)

      // the sync progress
      synchronizationProgressDAO.delete(blockhash)
      Good(())
    }
  }

  override def atomicRollback(blockhash: Blockhash): ApplicationResult[Unit] =
    withTransaction { implicit conn =>
      // transactions
      val deletedTransactions = transactionDAO.deleteBy(blockhash)
      val _ = blockFilterDAO.delete(blockhash)
      blockRewardDAO.deleteBy(blockhash)
      val maybe = for {
        // block
        _ <- blockDAO.delete(blockhash)

        // balances
        balanceList = TransactionBalancesHelper.computeBalances(
          deletedTransactions
        )
        _ <- balanceList
          .map { b =>
            b.copy(spent = -b.spent, received = -b.received)
          }
          .map { b =>
            balanceDAO.upsert(b)
          }
          .toList
          .everything

        // compute aggregated amount
        delta = balanceList.map(_.available).sum
        _ = aggregatedAmountDAO.updateAvailableCoins(-delta)
        _ = synchronizationProgressDAO.delete(blockhash)
      } yield ()

      maybe
        .map(x => Good(x))
        .getOrElse(
          throw new RuntimeException(s"Failed to rollback block $blockhash")
        )
    }

  override def upsertBlockReward(
      blockhash: Blockhash,
      reward: Option[BlockRewards]
  ): ApplicationResult[Unit] = {
    withConnection { implicit conn =>
      reward
        .map(BlockRewardPostgresSerializer.serialize)
        .foreach(_.foreach(r => blockRewardDAO.upsert(blockhash, r)))

      Good(())
    }
  }
}
