package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.{BlockFutureDataHandler, LedgerFutureDataHandler, TransactionFutureDataHandler}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.helpers._
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Height}
import com.xsn.explorer.parsers.TransactionOrderingParser
import org.scalactic.{Bad, Good, One, Or}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class LedgerSynchronizerServiceSpec extends PostgresDataHandlerSpec with BeforeAndAfter with ScalaFutures {

  lazy val dataHandler = createLedgerDataHandler(database)
  lazy val transactionDataHandler = createTransactionDataHandler(database)
  lazy val blockDataHandler = createBlockDataHandler(database)

  val blockList = List(
    BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34"),
    BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7"),
    BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8"),
    BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd"),
    BlockLoader.get("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32"),
    BlockLoader.get("00000267225f7dba55d9a3493740e7f0dde0f28a371d2c3b42e7676b5728d020"),
    BlockLoader.get("0000017ee4121cd8ae22f7321041ccb953d53828824217a9dc61a1c857facf85")
  )

  val genesis = blockList(0)

  before {
    clearDatabase()
  }

  "synchronize" should {
    "add the genensis block to the empty ledger" in {
      val synchronizer = ledgerSynchronizerService(genesis)

      whenReady(synchronizer.synchronize(genesis.hash)) { result =>
        result mustEqual Good(())
        verifyLedger(genesis)
      }
    }

    "add the old missing blocks  blocks while adding block N to the empty ledger" in {
      val block = blockList.last
      val synchronizer = ledgerSynchronizerService(blockList: _*)
      whenReady(synchronizer.synchronize(block.hash)) { result =>
        result mustEqual Good(())
        verifyLedger(blockList: _*)
      }
    }

    "append a block to the latest block" in {
      val synchronizer = ledgerSynchronizerService(blockList: _*)

      whenReady(synchronizer.synchronize(genesis.hash)) { _ mustEqual Good(()) }

      blockList.drop(1).foreach { block =>
        whenReady(synchronizer.synchronize(block.hash)) { _ mustEqual Good(()) }
      }

      verifyLedger(blockList: _*)
    }

    "ignore a duplicated block" in {
      val synchronizer = ledgerSynchronizerService(blockList: _*)

      createBlocks(synchronizer, blockList: _*)

      val block = blockList(3)
      whenReady(synchronizer.synchronize(block.hash)) { _ mustEqual Good(()) }

      verifyLedger(blockList: _*)
    }

    "add the old missing blocks  blocks while adding block N to a ledger with some blocks" in {
      val initialBlocks = blockList.take(3)
      val synchronizer = ledgerSynchronizerService(blockList: _*)

      createBlocks(synchronizer, initialBlocks: _*)

      val block = blockList.last
      whenReady(synchronizer.synchronize(block.hash)) { result =>
        result mustEqual Good(())
        verifyLedger(blockList: _*)
      }
    }

    "handle reorganization, ledger has 3 blocks, a rechain occurs from block 2 while adding new block 3" in {
      val block1 = blockList(1)
      val block2 = blockList(2)
      val block3 = blockList(3)
      val newBlock2 = blockList(4).copy(previousBlockhash = block2.previousBlockhash, height = block2.height)
      val newBlock3 = blockList(5).copy(previousBlockhash = Some(newBlock2.hash), height = Height(3))

      val initialBlocks = List(genesis, block1, block2, block3)
      createBlocks(ledgerSynchronizerService(initialBlocks: _*), initialBlocks: _*)

      val finalBlocks = List(
        genesis,
        block1.copy(nextBlockhash = Some(newBlock2.hash)),
        newBlock2.copy(nextBlockhash = Some(newBlock3.hash)),
        newBlock3)

      val synchronizer = ledgerSynchronizerService(finalBlocks: _*)
      whenReady(synchronizer.synchronize(newBlock3.hash)) { result =>
        result mustEqual Good(())
        verifyLedger(finalBlocks: _*)
      }
    }

    "handle reorganization, ledger has 3 blocks, a rechain occurs from block 2 while adding new block 4" in {
      val block1 = blockList(1)
      val block2 = blockList(2)
      val block3 = blockList(3)
      val newBlock2 = blockList(4).copy(previousBlockhash = block2.previousBlockhash, height = block2.height)
      val newBlock3 = blockList(5).copy(previousBlockhash = Some(newBlock2.hash), height = Height(3))
      val newBlock4 = blockList(6).copy(previousBlockhash = Some(newBlock3.hash), height = Height(4))

      val initialBlocks = List(genesis, block1, block2, block3)
      createBlocks(ledgerSynchronizerService(initialBlocks: _*), initialBlocks: _*)

      val finalBlocks = List(
        genesis,
        block1.copy(nextBlockhash = Some(newBlock2.hash)),
        newBlock2.copy(nextBlockhash = Some(newBlock3.hash)),
        newBlock3.copy(nextBlockhash = Some(newBlock4.hash)),
        newBlock4)

      val synchronizer = ledgerSynchronizerService(finalBlocks: _*)
      whenReady(synchronizer.synchronize(newBlock4.hash)) { result =>
        result mustEqual Good(())
        verifyLedger(finalBlocks: _*)
      }
    }

    "handle reorganization, ledger has 6 blocks, a rechain occurs from block 2 while adding new block 2" in {
      val initialBlocks = blockList.take(6)
      createBlocks(ledgerSynchronizerService(initialBlocks: _*), initialBlocks: _*)

      val block1 = blockList(1)
      val newBlock2 = blockList.drop(6).head.copy(previousBlockhash = Some(block1.hash), height = Height(2))
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

  private def verifyLedger(blocks: Block*) = {
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

  private def createBlocks(synchronizer: LedgerSynchronizerService, blocks: Block*) = {
    blocks
        .foreach { block =>
          whenReady(synchronizer.synchronize(block.hash)) { result =>
            result.isGood mustEqual true
          }
        }
  }

  private def ledgerSynchronizerService(blocks: Block*): LedgerSynchronizerService = {
    val xsnService = new FileBasedXSNService {
      override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block] = {
        blocks
            .find(_.hash == blockhash)
            .map { block => Future.successful(Good(cleanGenesisBlock(block))) }
            .getOrElse {
              Future.successful(Bad(BlockNotFoundError).accumulating)
            }
      }

      override def getLatestBlock(): FutureApplicationResult[Block] = {
        val block = cleanGenesisBlock(blocks.maxBy(_.height.int))
        Future.successful(Good(block))
      }

      override def getBlockhash(height: Height): FutureApplicationResult[Blockhash] = {
        val maybe = blocks.find(_.height == height).map(_.hash)
        val result = Or.from(maybe, One(BlockNotFoundError))
        Future.successful(result)
      }
    }

    ledgerSynchronizerService(xsnService)
  }

  private def ledgerSynchronizerService(xsnService: XSNService): LedgerSynchronizerService = {
    val transactionService = new TransactionService(
      new PaginatedQueryValidator,
      new TransactionOrderingParser,
      xsnService,
      new TransactionFutureDataHandler(transactionDataHandler)(Executors.databaseEC))

    new LedgerSynchronizerService(
      xsnService,
      transactionService,
      new LedgerFutureDataHandler(dataHandler)(Executors.databaseEC),
      new BlockFutureDataHandler(blockDataHandler)(Executors.databaseEC))
  }
}
