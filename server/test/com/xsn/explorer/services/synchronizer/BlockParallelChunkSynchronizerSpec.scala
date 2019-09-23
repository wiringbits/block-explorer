package com.xsn.explorer.services.synchronizer

import java.sql.Connection

import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers.DataGenerator
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.values.Blockhash
import com.xsn.explorer.models.{BlockExtractionMethod, TPoSContract}
import com.xsn.explorer.services.synchronizer.operations.BlockParallelChunkAddOps
import com.xsn.explorer.services.synchronizer.repository.{BlockChunkRepository, BlockSynchronizationProgressDAO}
import io.scalaland.chimney.dsl._
import org.scalactic.Good
import org.scalatest.{BeforeAndAfter, WordSpec}

class BlockParallelChunkSynchronizerSpec extends WordSpec with PostgresDataHandlerSpec with BeforeAndAfter {

  private val emptyFilterFactory = () => GolombCodedSet(1, 2, 3, List(new UnsignedByte(0.toByte)))

  lazy val blockDataHandler = createBlockDataHandler(database)
  lazy val transactionDataHandler = createTransactionDataHandler(database)
  lazy val tposContractDataHandler = createTPoSContractDataHandler(database)

  before {
    clearDatabase()
  }

  val block = DataGenerator
    .randomBlock()
    .into[Block]
    .withFieldConst(_.extractionMethod, BlockExtractionMethod.ProofOfWork)
    .transform

  val tx1 = {
    val txid = DataGenerator.randomTransactionId
    val outputs = DataGenerator.randomOutputs(10, txid)
    DataGenerator
      .randomTransaction(
        blockhash = block.hash,
        id = txid,
        utxos = List.empty
      )
      .copy(outputs = outputs)
  }

  val tx2 = {
    val txid = DataGenerator.randomTransactionId
    DataGenerator
      .randomTransaction(
        blockhash = block.hash,
        id = txid,
        utxos = tx1.outputs
      )
  }

  val tx3 = {
    val txid = DataGenerator.randomTransactionId
    DataGenerator
      .randomTransaction(
        blockhash = block.hash,
        id = txid,
        utxos = tx2.outputs
      )
  }

  val blockWithTransactions = Block.HasTransactions(block, List(tx1, tx2, tx3))

  val tposContracts = List(
    DataGenerator.randomTPoSContract(tx1.id, 0),
    DataGenerator.randomTPoSContract(tx2.id, 0),
    DataGenerator.randomTPoSContract(tx3.id, 0)
  ).map(_.copy(state = TPoSContract.State.Active))

  "sync" should {
    "sync a block" in {
      val synchronizer = createSynchronizer()
      whenReady(synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, None)) { result =>
        result must be(Good(()))
        verifyDatabase(blockWithTransactions, tposContracts)
      }
    }

    BlockSynchronizationState.values.foreach { state =>
      s"complete syncing a block after it failed on the $state phase" in {
        val dao = daoFailingOnceAt(state)
        val synchronizer = createSynchronizer(dao)
        intercept[RuntimeException] {
          synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, None).futureValue
        }

        whenReady(synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, None)) { result =>
          result must be(Good(()))
          verifyDatabase(blockWithTransactions, tposContracts)
        }
      }
    }
  }

  "rollback" should {
    "do nothing when the block is not synced" in {
      val result = createSynchronizer().rollback(block.hash).futureValue
      result must be(Good(()))
    }

    BlockSynchronizationState.values.foreach { state =>
      s"rollback a block after it failed on the $state phase" in {
        val dao = daoFailingOnceAt(state)
        val synchronizer = createSynchronizer(dao)
        intercept[RuntimeException] {
          synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, None).futureValue
        }

        whenReady(synchronizer.rollback(block.hash)) { result =>
          result must be(Good(()))
        }
      }
    }
  }

  def daoFailingOnceAt(givenState: BlockSynchronizationState): BlockSynchronizationProgressDAO = {
    new BlockSynchronizationProgressDAO {
      private var times = 0
      override def upsert(blockhash: Blockhash, state: BlockSynchronizationState)(implicit conn: Connection): Unit = {
        if (times == 0 && givenState == state) {
          times = 1
          throw new RuntimeException(s"Failed at ${givenState.entryName}")
        } else {
          super.upsert(blockhash, state)
        }
      }
    }
  }

  private def createSynchronizer(
      blockSyncProgressDAO: BlockSynchronizationProgressDAO = blockSynchronizationProgressDAO
  ): BlockParallelChunkSynchronizer = {
    val blockChunkRepository = createBlockChunkRepository(database, blockSyncProgressDAO)
    val blockChunkFutureRepository = new BlockChunkRepository.FutureImpl(blockChunkRepository)
    val addOps = new BlockParallelChunkAddOps(blockChunkFutureRepository)
    val blockParallelChunkSynchronizer =
      new BlockParallelChunkSynchronizer(blockChunkFutureRepository, addOps)
    blockParallelChunkSynchronizer
  }

  private def verifyDatabase(block: Block.HasTransactions, tposContracts: List[TPoSContract]) = {
    blockDataHandler.getBy(block.hash) must be(Good(block.block))
    transactionDataHandler.getTransactionsWithIOBy(block.hash, Limit(100), None) must be(Good(block.transactions))
    tposContracts.foreach { contract =>
      val result = tposContractDataHandler.getBy(contract.details.owner)
      result.isGood must be(true)
      result.get.contains(contract) must be(true)
    }
  }
}
