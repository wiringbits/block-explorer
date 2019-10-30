package com.xsn.explorer.services.synchronizer.repository

import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.{BlockRewards, TPoSContract}
import com.xsn.explorer.models.persisted.{AddressTransactionDetails, Balance, Block, Transaction}
import com.xsn.explorer.models.values.{Blockhash, TransactionId}
import com.xsn.explorer.services.synchronizer.BlockSynchronizationState
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

trait BlockChunkRepository[F[_]] {

  def upsertSyncState(blockhash: Blockhash, state: BlockSynchronizationState): F[Unit]

  def findSyncState(blockhash: Blockhash): F[Option[BlockSynchronizationState]]

  def findSyncingBlock(): F[Option[Blockhash]]

  // blocks
  def upsertBlock(block: Block): F[Unit]

  def upsertFilter(blockhash: Blockhash, filter: GolombCodedSet): F[Unit]

  def setNextBlockhash(blockhash: Blockhash, nextBlockhash: Blockhash): F[Unit]

  // transactions
  def upsertTransaction(index: Int, transaction: Transaction): F[Unit]

  def upsertInput(txid: TransactionId, input: Transaction.Input): F[Unit]

  def upsertOutput(output: Transaction.Output): F[Unit]

  def spendOutput(txid: TransactionId, index: Int, spentOn: TransactionId): F[Unit]

  def upsertAddressTransactionDetails(details: AddressTransactionDetails): F[Unit]

  // contracts
  def closeContract(id: TPoSContract.Id, closedOn: TransactionId): F[Unit]

  def upsertContract(contract: TPoSContract): F[Unit]

  /**
   * Atomically do the following:
   * - Update the given balance list
   * - Based on the balance list, update the available coins
   * - Mark the block synchronization as complete (deleting the row)
   */
  def atomicUpdateBalances(blockhash: Blockhash, balances: List[Balance]): F[Unit]

  /**
   * Rollback a block atomically, the sync status must be deleted too.
   */
  def atomicRollback(blockhash: Blockhash): F[Unit]

  def upsertBlockReward(blockhash: Blockhash, reward: Option[BlockRewards]): F[Unit]
}

object BlockChunkRepository {

  trait Blocking extends BlockChunkRepository[ApplicationResult]

  class FutureImpl @Inject()(blocking: Blocking)(implicit ec: ExecutionContext)
      extends BlockChunkRepository[FutureApplicationResult] {

    override def upsertSyncState(
        blockhash: Blockhash,
        state: BlockSynchronizationState
    ): FutureApplicationResult[Unit] = Future {
      blocking.upsertSyncState(blockhash, state)
    }

    override def findSyncState(blockhash: Blockhash): FutureApplicationResult[Option[BlockSynchronizationState]] = {
      Future {
        blocking.findSyncState(blockhash)
      }
    }

    override def findSyncingBlock(): FutureApplicationResult[Option[Blockhash]] = Future {
      blocking.findSyncingBlock()
    }

    override def upsertBlock(block: Block): FutureApplicationResult[Unit] = Future {
      blocking.upsertBlock(block)
    }

    override def upsertFilter(blockhash: Blockhash, filter: GolombCodedSet): FutureApplicationResult[Unit] = Future {
      blocking.upsertFilter(blockhash, filter)
    }

    override def setNextBlockhash(blockhash: Blockhash, nextBlockhash: Blockhash): FutureApplicationResult[Unit] = {
      Future {
        blocking.setNextBlockhash(blockhash, nextBlockhash)
      }
    }

    override def upsertTransaction(index: Int, transaction: Transaction): FutureApplicationResult[Unit] = Future {
      blocking.upsertTransaction(index, transaction)
    }

    override def upsertInput(txid: TransactionId, input: Transaction.Input): FutureApplicationResult[Unit] = Future {
      blocking.upsertInput(txid, input)
    }

    override def upsertOutput(output: Transaction.Output): FutureApplicationResult[Unit] = Future {
      blocking.upsertOutput(output)
    }

    override def spendOutput(txid: TransactionId, index: Int, spentOn: TransactionId): FutureApplicationResult[Unit] = {
      Future {
        blocking.spendOutput(txid, index, spentOn)
      }
    }

    override def upsertAddressTransactionDetails(details: AddressTransactionDetails): FutureApplicationResult[Unit] = {
      Future {
        blocking.upsertAddressTransactionDetails(details)
      }
    }

    override def closeContract(id: TPoSContract.Id, closedOn: TransactionId): FutureApplicationResult[Unit] = Future {
      blocking.closeContract(id, closedOn)
    }

    override def upsertContract(contract: TPoSContract): FutureApplicationResult[Unit] = Future {
      blocking.upsertContract(contract)
    }

    override def atomicUpdateBalances(blockhash: Blockhash, balances: List[Balance]): FutureApplicationResult[Unit] = {
      Future {
        blocking.atomicUpdateBalances(blockhash, balances)
      }
    }

    override def atomicRollback(blockhash: Blockhash): FutureApplicationResult[Unit] = Future {
      blocking.atomicRollback(blockhash)
    }

    override def upsertBlockReward(blockhash: Blockhash, reward: Option[BlockRewards]): FutureApplicationResult[Unit] =
      Future {
        blocking.upsertBlockReward(blockhash, reward)
      }
  }
}
