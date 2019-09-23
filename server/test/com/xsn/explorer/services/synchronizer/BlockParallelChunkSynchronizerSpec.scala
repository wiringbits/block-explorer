package com.xsn.explorer.services.synchronizer

import java.sql.Connection

import com.alexitc.playsonify.models.pagination.Limit
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers.DataGenerator
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.values.Blockhash
import com.xsn.explorer.models.{
  BlockExtractionMethod,
  BlockReward,
  BlockRewards,
  PoSBlockRewards,
  PoWBlockRewards,
  TPoSBlockRewards,
  TPoSContract
}
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

  val reward = PoWBlockRewards(BlockReward(DataGenerator.randomAddress, 100))

  val tposContracts = List(
    DataGenerator.randomTPoSContract(tx1.id, 0),
    DataGenerator.randomTPoSContract(tx2.id, 0),
    DataGenerator.randomTPoSContract(tx3.id, 0)
  ).map(_.copy(state = TPoSContract.State.Active))

  "sync" should {
    "sync a block" in {
      val synchronizer = createSynchronizer()
      whenReady(synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, reward)) { result =>
        result must be(Good(()))
        verifyDatabase(blockWithTransactions, tposContracts, None)
      }
    }

    "store PoW reward" in {
      val powBlock = blockWithTransactions.copy(
        block = blockWithTransactions.block.copy(extractionMethod = BlockExtractionMethod.ProofOfWork)
      )
      val powReward = PoWBlockRewards(BlockReward(DataGenerator.randomAddress, 1000))
      val synchronizer = createSynchronizer()
      whenReady(synchronizer.sync(powBlock, tposContracts, emptyFilterFactory, powReward)) { result =>
        result must be(Good(()))
        verifyDatabase(powBlock, tposContracts, Some(powReward))
      }
    }

    "store PoS reward" in {
      val posBlock = blockWithTransactions.copy(
        block = blockWithTransactions.block.copy(extractionMethod = BlockExtractionMethod.ProofOfStake)
      )
      val posReward = PoSBlockRewards(BlockReward(DataGenerator.randomAddress, 1000), None)
      val synchronizer = createSynchronizer()
      whenReady(synchronizer.sync(posBlock, tposContracts, emptyFilterFactory, posReward)) { result =>
        result must be(Good(()))
        verifyDatabase(posBlock, tposContracts, Some(posReward))
      }
    }

    "store TPoS reward" in {
      val tposBlock = blockWithTransactions.copy(
        block = blockWithTransactions.block.copy(extractionMethod = BlockExtractionMethod.TrustlessProofOfStake)
      )
      val ownerReward = BlockReward(DataGenerator.randomAddress, 1000)
      val merchantReward = BlockReward(DataGenerator.randomAddress, 100)
      val tposReward = TPoSBlockRewards(ownerReward, merchantReward, None)
      val synchronizer = createSynchronizer()
      whenReady(synchronizer.sync(tposBlock, tposContracts, emptyFilterFactory, tposReward)) { result =>
        result must be(Good(()))
        verifyDatabase(tposBlock, tposContracts, Some(tposReward))
      }
    }

    BlockSynchronizationState.values.foreach { state =>
      s"complete syncing a block after it failed on the $state phase" in {
        val dao = daoFailingOnceAt(state)
        val synchronizer = createSynchronizer(dao)
        intercept[RuntimeException] {
          synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, reward).futureValue
        }

        whenReady(synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, reward)) { result =>
          result must be(Good(()))
          verifyDatabase(blockWithTransactions, tposContracts, None)
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
          synchronizer.sync(blockWithTransactions, tposContracts, emptyFilterFactory, reward).futureValue
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

  private def verifyDatabase(
      block: Block.HasTransactions,
      tposContracts: List[TPoSContract],
      reward: Option[BlockRewards]
  ) = {
    blockDataHandler.getBy(block.hash) must be(Good(block.block))
    transactionDataHandler.getTransactionsWithIOBy(block.hash, Limit(100), None) must be(Good(block.transactions))
    tposContracts.foreach { contract =>
      val result = tposContractDataHandler.getBy(contract.details.owner)
      result.isGood must be(true)
      result.get.contains(contract) must be(true)
    }

    reward match {
      case Some(r: PoWBlockRewards) => verifyPoWReward(block.hash, r)
      case Some(r: PoSBlockRewards) => verifyPoSReward(block.hash, r)
      case Some(r: TPoSBlockRewards) => verifyTPoSReward(block.hash, r)
      case _ => succeed
    }
  }

  private def verifyPoWReward(blockhash: Blockhash, powReward: PoWBlockRewards) = {
    database.withConnection { implicit conn =>
      val reward = blockRewardPostgresDAO.getBy(blockhash)
      reward match {
        case r: PoWBlockRewards => {
          r.reward.address mustEqual powReward.reward.address
          r.reward.value mustEqual powReward.reward.value
        }
        case _ => fail
      }
    }
  }

  private def verifyPoSReward(blockhash: Blockhash, posReward: PoSBlockRewards) = {
    database.withConnection { implicit conn =>
      val reward = blockRewardPostgresDAO.getBy(blockhash)
      reward match {
        case r: PoSBlockRewards => {
          r.coinstake.address mustEqual posReward.coinstake.address
          r.coinstake.value mustEqual posReward.coinstake.value
        }
        case _ => fail
      }
    }
  }

  private def verifyTPoSReward(blockhash: Blockhash, tposReward: TPoSBlockRewards) = {
    database.withConnection { implicit conn =>
      val reward = blockRewardPostgresDAO.getBy(blockhash)
      reward match {
        case r: TPoSBlockRewards => {
          r.owner.address mustEqual tposReward.owner.address
          r.owner.value mustEqual tposReward.owner.value

          r.merchant.address mustEqual tposReward.merchant.address
          r.merchant.value mustEqual tposReward.merchant.value
        }
        case _ => fail
      }
    }
  }
}
