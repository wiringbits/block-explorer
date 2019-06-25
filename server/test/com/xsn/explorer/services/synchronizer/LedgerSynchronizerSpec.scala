package com.xsn.explorer.services.synchronizer

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.cache.BlockHeaderCache
import com.xsn.explorer.data.async.{BlockFutureDataHandler, LedgerFutureDataHandler, TransactionFutureDataHandler}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.helpers.LedgerHelper._
import com.xsn.explorer.helpers._
import com.xsn.explorer.models.rpc.{Block, TransactionVIN}
import com.xsn.explorer.models.values.{Blockhash, Height}
import com.xsn.explorer.parsers.OrderingConditionParser
import com.xsn.explorer.services.logic.{BlockLogic, TransactionLogic}
import com.xsn.explorer.services.validators.BlockhashValidator
import com.xsn.explorer.services.{BlockService, TransactionCollectorService, XSNService}
import org.scalactic.{Bad, Good, One, Or}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class LedgerSynchronizerSpec extends PostgresDataHandlerSpec with BeforeAndAfter with ScalaFutures {

  lazy val dataHandler = createLedgerDataHandler(database)
  lazy val transactionDataHandler = createTransactionDataHandler(database)
  lazy val blockDataHandler = createBlockDataHandler(database)

  val genesis = fullBlockList.head

  before {
    clearDatabase()
  }

  testWith("legacy")(legacyConstructor)
  testWith("new")(newConstructor)

  private def testWith(tag: String)(implicit constructor: XSNService => LedgerSynchronizer): Unit = {
    s"synchronize - $tag" should {
      "add the genensis block to the empty ledger" in {
        val synchronizer = ledgerSynchronizerService(genesis)

        whenReady(synchronizer.synchronize(genesis.hash)) { result =>
          result mustEqual Good(())
          verifyLedger(genesis)
        }
      }

      "add the old missing blocks  blocks while adding block N to the empty ledger" in {
        val block = fullBlockList.last
        val synchronizer = ledgerSynchronizerService(fullBlockList: _*)
        whenReady(synchronizer.synchronize(block.hash)) { result =>
          result mustEqual Good(())
          verifyLedger(fullBlockList: _*)
        }
      }

      "append a block to the latest block" in {
        val synchronizer = ledgerSynchronizerService(fullBlockList: _*)

        whenReady(synchronizer.synchronize(genesis.hash)) {
          _ mustEqual Good(())
        }

        fullBlockList.drop(1).foreach { block =>
          whenReady(synchronizer.synchronize(block.hash)) {
            _ mustEqual Good(())
          }
        }

        verifyLedger(fullBlockList: _*)
      }

      "ignore a duplicated block" in {
        val synchronizer = ledgerSynchronizerService(fullBlockList: _*)

        createBlocks(synchronizer, fullBlockList: _*)

        val block = fullBlockList(3)
        whenReady(synchronizer.synchronize(block.hash)) {
          _ mustEqual Good(())
        }

        verifyLedger(blockList: _*)
      }

      "add the old missing blocks  blocks while adding block N to a ledger with some blocks" in {
        val initialBlocks = fullBlockList.take(3)
        val synchronizer = ledgerSynchronizerService(fullBlockList: _*)

        createBlocks(synchronizer, initialBlocks: _*)

        val block = fullBlockList.last
        whenReady(synchronizer.synchronize(block.hash)) { result =>
          result mustEqual Good(())
          verifyLedger(blockList: _*)
        }
      }

      "handle reorganization, ledger has 3 blocks, a rechain occurs from block 2 while adding new block 3" in {
        val block1 = fullBlockList(1)
        val block2 = fullBlockList(2)
        val block3 = fullBlockList(3)
        val newBlock2 =
          fullBlockList(4).copy(previousBlockhash = Some(block1.hash), height = Height(block1.height.int + 1))
        val newBlock3 =
          fullBlockList(5).copy(previousBlockhash = Some(newBlock2.hash), height = Height(newBlock2.height.int + 1))

        val initialBlocks = List(genesis, block1, block2, block3)
        createBlocks(ledgerSynchronizerService(initialBlocks: _*), initialBlocks: _*)

        val finalBlocks = List(
          genesis,
          block1.copy(nextBlockhash = Some(newBlock2.hash)),
          newBlock2.copy(nextBlockhash = Some(newBlock3.hash)),
          newBlock3
        )

        val synchronizer = ledgerSynchronizerService(finalBlocks: _*)
        whenReady(synchronizer.synchronize(newBlock3.hash)) { result =>
          result mustEqual Good(())
          verifyLedger(finalBlocks: _*)
        }
      }

      "handle reorganization, ledger has 3 blocks, a rechain occurs from block 2 while adding new block 4" in {
        val block1 = fullBlockList(1)
        val block2 = fullBlockList(2)
        val block3 = fullBlockList(3)
        val newBlock2 = fullBlockList(4).copy(previousBlockhash = block2.previousBlockhash, height = block2.height)
        val newBlock3 = fullBlockList(5).copy(previousBlockhash = Some(newBlock2.hash), height = Height(3))
        val newBlock4 = fullBlockList(6).copy(previousBlockhash = Some(newBlock3.hash), height = Height(4))

        val initialBlocks = List(genesis, block1, block2, block3)
        createBlocks(ledgerSynchronizerService(initialBlocks: _*), initialBlocks: _*)

        val finalBlocks = List(
          genesis,
          block1.copy(nextBlockhash = Some(newBlock2.hash)),
          newBlock2.copy(nextBlockhash = Some(newBlock3.hash)),
          newBlock3.copy(nextBlockhash = Some(newBlock4.hash)),
          newBlock4
        )

        val synchronizer = ledgerSynchronizerService(finalBlocks: _*)
        whenReady(synchronizer.synchronize(newBlock4.hash)) { result =>
          result mustEqual Good(())
          verifyLedger(finalBlocks: _*)
        }
      }

      "handle reorganization, ledger has 6 blocks, a rechain occurs from block 2 while adding new block 2" in {
        val initialBlocks = fullBlockList.take(6)
        createBlocks(ledgerSynchronizerService(initialBlocks: _*), initialBlocks: _*)

        val block1 = fullBlockList(1)
        val newBlock2 = fullBlockList.drop(6).head.copy(previousBlockhash = Some(block1.hash), height = Height(2))
        val finalBlocks = List(
          genesis,
          block1.copy(nextBlockhash = Some(newBlock2.hash)),
          newBlock2
        )

        val synchronizer = ledgerSynchronizerService(finalBlocks: _*)
        whenReady(synchronizer.synchronize(newBlock2.hash)) { result =>
          result mustEqual Good(())
          verifyLedger(finalBlocks: _*)
        }
      }
    }
  }

  private def verifyLedger(blocks: Block[_]*): Unit = {
    countBlocks() mustEqual blocks.size
    blocks.foreach { block =>
      val dbBlock = blockDataHandler.getBy(block.hash).get

      dbBlock.height mustEqual block.height
      dbBlock.previousBlockhash mustEqual block.previousBlockhash
      if (block == blocks.last) {
        dbBlock.nextBlockhash.isEmpty mustEqual true
      } else {
        dbBlock.nextBlockhash mustEqual block.nextBlockhash
      }
    }
  }

  private def countBlocks() = {
    database.withConnection { implicit conn =>
      _root_.anorm.SQL("""SELECT COUNT(*) FROM blocks""").as(_root_.anorm.SqlParser.scalar[Int].single)
    }
  }

  private def createBlocks(synchronizer: LedgerSynchronizer, blocks: Block[_]*): Unit = {
    blocks
      .foreach { block =>
        whenReady(synchronizer.synchronize(block.hash)) { result =>
          result.isGood mustEqual true
        }
      }
  }

  private def ledgerSynchronizerService(
      blocks: Block.HasTransactions[TransactionVIN]*
  )(implicit constructor: XSNService => LedgerSynchronizer): LedgerSynchronizer = {

    import io.scalaland.chimney.dsl._

    val canonicalBlocks = blocks.map { fullBlock =>
      val block = fullBlock
        .into[Block.Canonical]
        .withFieldComputed(_.transactions, _.transactions.map(_.id))
        .transform

      block
    }

    val xsnService = new FileBasedXSNService {
      override def getFullBlock(
          blockhash: Blockhash
      ): FutureApplicationResult[Block.HasTransactions[TransactionVIN]] = {
        blocks
          .find(_.hash == blockhash)
          .map { block =>
            Future.successful(Good(block))
          }
          .getOrElse {
            Future.successful(Bad(BlockNotFoundError).accumulating)
          }
      }

      override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block.Canonical] = {
        canonicalBlocks
          .find(_.hash == blockhash)
          .map { block =>
            Future.successful(Good(cleanGenesisBlock(block)))
          }
          .getOrElse {
            Future.successful(Bad(BlockNotFoundError).accumulating)
          }
      }

      override def getLatestBlock(): FutureApplicationResult[Block.Canonical] = {
        val block = cleanGenesisBlock(canonicalBlocks.maxBy(_.height.int))
        Future.successful(Good(block))
      }

      override def getBlockhash(height: Height): FutureApplicationResult[Blockhash] = {
        val maybe = blocks.find(_.height == height).map(_.hash)
        val result = Or.from(maybe, One(BlockNotFoundError))
        Future.successful(result)
      }
    }

    constructor(xsnService)
  }

  private def newConstructor(xsnService: XSNService): LedgerSynchronizer = {
    val blockFutureDataHandler = new BlockFutureDataHandler(blockDataHandler)(Executors.databaseEC)
    val blockService = new BlockService(
      xsnService,
      blockFutureDataHandler,
      new PaginatedQueryValidator,
      new BlockhashValidator,
      new BlockLogic,
      new TransactionLogic,
      new OrderingConditionParser,
      BlockHeaderCache.default
    )
    val transactionCollectorService = new TransactionCollectorService(
      xsnService,
      new TransactionFutureDataHandler(transactionDataHandler)(Executors.databaseEC)
    )

    val syncOps = new LedgerSynchronizationOps(
      Config.explorerConfig,
      blockFutureDataHandler,
      xsnService,
      blockService,
      transactionCollectorService
    )
    val syncStatusService = new LedgerSynchronizationStatusService(syncOps, xsnService, blockFutureDataHandler)
    new LedgerSynchronizerService(
      xsnService,
      new LedgerFutureDataHandler(dataHandler)(Executors.databaseEC),
      syncStatusService,
      syncOps
    )
  }

  private def legacyConstructor(xsnService: XSNService): LedgerSynchronizer = {
    val blockFutureDataHandler = new BlockFutureDataHandler(blockDataHandler)(Executors.databaseEC)
    val blockService = new BlockService(
      xsnService,
      blockFutureDataHandler,
      new PaginatedQueryValidator,
      new BlockhashValidator,
      new BlockLogic,
      new TransactionLogic,
      new OrderingConditionParser,
      BlockHeaderCache.default
    )
    val transactionCollectorService = new TransactionCollectorService(
      xsnService,
      new TransactionFutureDataHandler(transactionDataHandler)(Executors.databaseEC)
    )
    val syncOps = new LedgerSynchronizationOps(
      Config.explorerConfig,
      blockFutureDataHandler,
      xsnService,
      blockService,
      transactionCollectorService
    )
    new LegacyLedgerSynchronizerService(
      Config.explorerConfig,
      xsnService,
      blockService,
      transactionCollectorService,
      new LedgerFutureDataHandler(dataHandler)(Executors.databaseEC),
      blockFutureDataHandler,
      syncOps
    )
  }
}
