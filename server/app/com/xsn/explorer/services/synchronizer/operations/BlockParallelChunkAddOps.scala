package com.xsn.explorer.services.synchronizer.operations

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.{BlockRewards, TPoSContract}
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.services.synchronizer.BlockSynchronizationState
import com.xsn.explorer.services.synchronizer.repository.BlockChunkRepository
import com.xsn.explorer.util.Extensions.FutureApplicationResultListExt
import com.xsn.explorer.util.{
  TransactionAddressesHelper,
  TransactionBalancesHelper
}
import javax.inject.Inject
import org.scalactic.Good

import scala.concurrent.{ExecutionContext, Future}

class BlockParallelChunkAddOps @Inject() (
    blockChunkRepository: BlockChunkRepository.FutureImpl
)(implicit
    ec: ExecutionContext
) {

  def continueFromState(
      state: BlockSynchronizationState,
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = state match {
    case BlockSynchronizationState.StoringBlock =>
      storeBlock(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.StoringBlockData =>
      storeBlockData(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.StoringTransactions =>
      storeTransactions(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.StoringOutputs =>
      storeOutputs(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.StoringInputs =>
      storeInputs(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.SpendingOutputs =>
      spendOutputs(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.StoringAddressTransactionDetails =>
      storeAddressTransactionDetails(
        block,
        tposContracts,
        filterFactory,
        rewards
      )
    case BlockSynchronizationState.UpdatingTPoSContracts =>
      updateTPoSContracts(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.StoringRewards =>
      storeRewards(block, tposContracts, filterFactory, rewards)
    case BlockSynchronizationState.UpdatingBalances =>
      updateBalances(block)
  }

  /** - Marks the block as being synchronized.
    * - Creates or updates the block on the database.
    * - Marks the next state to continue the syncrhonization.
    */
  private def storeBlock(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val initialState = BlockSynchronizationState.StoringBlock
    val nextState = BlockSynchronizationState.StoringBlockData

    val result = for {
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, initialState)
        .toFutureOr
      _ <- blockChunkRepository.upsertBlock(block.block).toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** In parallel:
    * - Generate and store the block filter.
    * - Link the previous block to the new one.
    *
    * Then, mark the next state to continue synchronizing.
    */
  private def storeBlockData(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.StoringTransactions
    val filterF = Future { filterFactory() }
      .flatMap(blockChunkRepository.upsertFilter(block.hash, _))

    val linkF = block.previousBlockhash match {
      case None => Future.successful(Good(()))
      case Some(previousBlockhash) =>
        blockChunkRepository.setNextBlockhash(previousBlockhash, block.hash)
    }

    val result = for {
      _ <- filterF.toFutureOr
      _ <- linkF.toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Store all transactions on the transactions table in parallel.
    *
    * Then, mark the next state to continue synchronizing.
    */
  private def storeTransactions(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.StoringOutputs
    val storeTransactionsF = block.transactions.zipWithIndex.map {
      case (tx, index) =>
        blockChunkRepository.upsertTransaction(index, tx.transaction)
    }.sequence

    val result = for {
      _ <- storeTransactionsF.toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Store the transaction outputs on the transaction_outputs table in parallel.
    *
    * Then, mark the next state to continue synchronizing.
    */
  private def storeOutputs(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.StoringInputs
    val storeOutputsF =
      for (tx <- block.transactions; output <- tx.outputs)
        yield blockChunkRepository.upsertOutput(output)

    val result = for {
      _ <- storeOutputsF.sequence.toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Store the inputs on the transaction_inputs table in parallel.
    *
    * Then, mark the next state to continue synchronizing.
    */
  private def storeInputs(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.SpendingOutputs
    val storeInputsF =
      for (tx <- block.transactions; input <- tx.inputs)
        yield blockChunkRepository.upsertInput(tx.id, input)

    val result = for {
      _ <- storeInputsF.sequence.toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Spend the transaction inputs in parallel.
    *
    * Then, mark the next state to continue synchronizing.
    */
  private def spendOutputs(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.StoringAddressTransactionDetails
    val spendOutputsF =
      for (tx <- block.transactions; input <- tx.inputs)
        yield blockChunkRepository.spendOutput(
          input.fromTxid,
          input.fromOutputIndex,
          tx.id
        )

    val result = for {
      _ <- spendOutputsF.sequence.toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Store the transaction details for each involved address in parallel.
    *
    * Then, mark the next state to continue synchronizing.
    */
  private def storeAddressTransactionDetails(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.UpdatingTPoSContracts
    val storeDetailsF =
      for (
        tx <- block.transactions;
        details <- TransactionAddressesHelper.computeDetails(tx)
      )
        yield blockChunkRepository.upsertAddressTransactionDetails(details)

    val result = for {
      _ <- storeDetailsF.sequence.toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Creates the new contracts, and closes the old ones in parallel.
    *
    * Then, mark the next state to continue synchronizing.
    */
  private def updateTPoSContracts(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.StoringRewards

    val createContractsF = tposContracts.map { contract =>
      blockChunkRepository.upsertContract(contract)
    }

    // a block may open and close the same contract
    lazy val closeContractsF = for {
      tx <- block.transactions
      // a contract requires 1 XSN
      input <- tx.inputs if input.value == 1
    } yield {
      val id = TPoSContract.Id(input.fromTxid, input.fromOutputIndex)
      blockChunkRepository.closeContract(id, tx.id)
    }

    val result = for {
      _ <- createContractsF.sequence.toFutureOr
      _ <- closeContractsF.sequence.toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Stores the block rewards and then mark the next state to continue synchronizing.
    */
  private def storeRewards(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      filterFactory: () => GolombCodedSet,
      rewards: Option[BlockRewards]
  ): FutureApplicationResult[Unit] = {
    val nextState = BlockSynchronizationState.UpdatingBalances

    val result = for {
      _ <- blockChunkRepository
        .upsertBlockReward(block.hash, rewards)
        .toFutureOr
      _ <- blockChunkRepository
        .upsertSyncState(block.hash, nextState)
        .toFutureOr
      _ <- continueFromState(
        nextState,
        block,
        tposContracts,
        filterFactory,
        rewards
      ).toFutureOr
    } yield ()

    result.toFuture
  }

  /** Updates the address balances, available coins and completes the block synchronizaiton.
    */
  private def updateBalances(
      block: Block.HasTransactions
  ): FutureApplicationResult[Unit] = {
    val balanceList =
      TransactionBalancesHelper.computeBalances(block.transactions).toList
    blockChunkRepository.atomicUpdateBalances(block.hash, balanceList)
  }
}
