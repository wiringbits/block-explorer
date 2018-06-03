package com.xsn.explorer.processors

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models._
import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, BlockPostgresDAO, StatisticsPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, BlockPostgresDataHandler, DatabasePostgresSeeder, StatisticsPostgresDataHandler}
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.{BlockNotFoundError, TransactionNotFoundError}
import com.xsn.explorer.helpers.{BlockLoader, Executors, FileBasedXSNService}
import com.xsn.explorer.models.fields.BalanceField
import com.xsn.explorer.models.rpc.{Block, Transaction}
import com.xsn.explorer.models.{Blockhash, TransactionId}
import com.xsn.explorer.processors.BlockEventsProcessor.{MissingBlockIgnored, NewBlockAppended, RechainDone, ReplacedByBlockHeight}
import com.xsn.explorer.services.TransactionService
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class BlockEventsProcessorSpec extends PostgresDataHandlerSpec with ScalaFutures with BeforeAndAfter {

  lazy val dataHandler = new BlockPostgresDataHandler(database, new BlockPostgresDAO)
  lazy val dataSeeder = new DatabasePostgresSeeder(
    database,
    new BlockPostgresDAO,
    new TransactionPostgresDAO,
    new BalancePostgresDAO(new FieldOrderingSQLInterpreter))

  lazy val xsnService = new FileBasedXSNService
  lazy val processor = new BlockEventsProcessor(
    xsnService,
    new TransactionService(xsnService)(Executors.globalEC),
    new DatabaseFutureSeeder(dataSeeder)(Executors.databaseEC),
    new BlockFutureDataHandler(dataHandler)(Executors.databaseEC))

  before {
    clearDatabase()
  }

  "newLatestBlock" should {
    "fail on genesis block due to the missing transaction" in {
      // see https://github.com/X9Developers/XSN/issues/32
      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")

      whenReady(processor.processBlock(block0.hash)) { result =>
        result mustEqual Good(MissingBlockIgnored)
      }
    }

    "process first block" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")

      whenReady(processor.processBlock(block1.hash)) { result =>
        result.isGood mustEqual true
      }
    }

    "process a new block" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block1, block2).map(dataHandler.insert).foreach(_.isGood mustEqual true)

      whenReady(processor.processBlock(block3.hash)) { result =>
        result mustEqual Good(NewBlockAppended(block3))
        val blocks = List(block1, block2, block3)
        verifyBlockchain(blocks)
      }
    }

    "process a rechain" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block1, block2, block3).map(dataHandler.insert).foreach(_.isGood mustEqual true)

      whenReady(processor.processBlock(block2.hash)) {
        case Good(RechainDone(orphanBlock, newBlock)) =>
          orphanBlock.hash mustEqual block3.hash
          newBlock.hash mustEqual block2.hash

          val blocks = List(block1, block2)
          verifyBlockchain(blocks)

        case _ => fail()
      }
    }

    "process a missing block" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block2, block3).map(dataHandler.insert).foreach(_.isGood mustEqual true)

      whenReady(processor.processBlock(block1.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block1, block2, block3)
        verifyBlockchain(blocks)
      }
    }

    "processing a repeated missing block doesn't affect the balance" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block1, block2, block3).map(dataHandler.insert).foreach(_.isGood mustEqual true)

      whenReady(processor.processBlock(block1.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block1, block2, block3)
        verifyBlockchain(blocks)

        val statsDataHandler = new StatisticsPostgresDataHandler(database, new StatisticsPostgresDAO)
        val stats = statsDataHandler.getStatistics().get
        stats.totalSupply.isEmpty mustEqual true
        stats.circulatingSupply.isEmpty mustEqual true
      }
    }

    "process a block without spent index on transactions" in {
      val block = BlockLoader.get("000001ff95f22b8d82db14a5c5e9f725e8239e548be43c668766e7ddaee81924")

      whenReady(processor.processBlock(block.hash)) { result =>
        result.isGood mustEqual true

        val balanceDataHandler = new BalancePostgresDataHandler(database, new BalancePostgresDAO(new FieldOrderingSQLInterpreter))
        val balance = balanceDataHandler.get(
          PaginatedQuery(Offset(0), Limit(100)),
          FieldOrdering(BalanceField.Available, OrderingCondition.DescendingOrder))
            .get
            .data
            .find(_.address.string == "XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9")
            .get

        balance.spent mustEqual BigDecimal("76500000.000000000000000")
      }
    }

    "process a rechain without corrupting the balances table" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")

      List(block1, block2)
          .map(_.hash)
          .map(processor.processBlock)
          .foreach { whenReady(_) { _.isGood mustEqual true } }

      whenReady(processor.processBlock(block1.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block1)
        verifyBlockchain(blocks)

        val balanceDataHandler = new BalancePostgresDataHandler(database, new BalancePostgresDAO(new FieldOrderingSQLInterpreter))
        val balances = balanceDataHandler.get(
          PaginatedQuery(Offset(0), Limit(100)),
          FieldOrdering(BalanceField.Available, OrderingCondition.DescendingOrder))
            .get
            .data

        val balance = balances
            .find(_.address.string == "XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9")
            .get

        balance.received mustEqual BigDecimal(0)
        balance.spent mustEqual BigDecimal(0)
      }
    }

    // TODO: failed once on travis, might not be stable
    "ignore orphan block on rare rechain events when the rpc server doesn't have the block anymore" in {
      // see https://github.com/X9Developers/block-explorer/issues/6
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      val xsnService = new FileBasedXSNService {
        override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block] = {
          if (blockhash == block3.hash) {
            Future.successful(Bad(BlockNotFoundError).accumulating)
          } else {
            super.getBlock(blockhash)
          }
        }
      }

      val processor = new BlockEventsProcessor(
        xsnService,
        new TransactionService(xsnService)(Executors.globalEC),
        new DatabaseFutureSeeder(dataSeeder)(Executors.databaseEC),
        new BlockFutureDataHandler(dataHandler)(Executors.databaseEC))


      List(block1, block2)
          .map(_.hash)
          .map(processor.processBlock)
          .foreach { whenReady(_) { _.isGood mustEqual true } }

      /**
       * When processing the latest block, a rechain event has occurred in the rpc server which leads to
       * a case that we can't retrieve the block information, the block should be ignored.
       */
      whenReady(processor.processBlock(block3.hash)) { result =>
        result mustEqual Good(MissingBlockIgnored)
        val blocks = List(block1, block2)
        verifyBlockchain(blocks)
      }
    }

    "ignore orphan block on rare rechain events when the rpc server doesn't have the a transaction from the block anymore" in {
      // see https://github.com/X9Developers/block-explorer/issues/6
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      val xsnService = new FileBasedXSNService {
        override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction] = {
          if (txid == block3.transactions.last) {
            Future.successful(Bad(TransactionNotFoundError).accumulating)
          } else {
            super.getTransaction(txid)
          }
        }
      }

      val processor = new BlockEventsProcessor(
        xsnService,
        new TransactionService(xsnService)(Executors.globalEC),
        new DatabaseFutureSeeder(dataSeeder)(Executors.databaseEC),
        new BlockFutureDataHandler(dataHandler)(Executors.databaseEC))


      List(block1, block2)
          .map(_.hash)
          .map(processor.processBlock)
          .foreach { whenReady(_) { _.isGood mustEqual true } }

      /**
       * When processing the latest block, a rechain event has occurred in the rpc server which leads to
       * a case that we can't retrieve a specific transaction, the block should be ignored.
       */
      whenReady(processor.processBlock(block3.hash)) { result =>
        result mustEqual Good(MissingBlockIgnored)
        val blocks = List(block1, block2)
        verifyBlockchain(blocks)
      }
    }

    "remove orphan block on rare rechain events when the block height already exists" in {
      // see https://github.com/X9Developers/block-explorer/issues/6
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")
          .copy(height = block2.height)
          .copy(previousBlockhash = block2.previousBlockhash)

      val xsnService = new FileBasedXSNService {
        override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block] = {
          if (blockhash == block3.hash) {
            Future.successful(Good(block3))
          } else {
            super.getBlock(blockhash)
          }
        }
      }

      val processor = new BlockEventsProcessor(
        xsnService,
        new TransactionService(xsnService)(Executors.globalEC),
        new DatabaseFutureSeeder(dataSeeder)(Executors.databaseEC),
        new BlockFutureDataHandler(dataHandler)(Executors.databaseEC))

      List(block1, block2)
          .map(_.hash)
          .map(processor.processBlock)
          .foreach { whenReady(_) { _.isGood mustEqual true } }

      whenReady(processor.processBlock(block3.hash)) { result =>
        result mustEqual Good(ReplacedByBlockHeight)
        val blocks = List(block1.copy(nextBlockhash = Some(block3.hash)), block3)
        verifyBlockchain(blocks)

        // ensure block2 has been removed
        dataHandler.getBy(block2.hash) mustEqual Bad(BlockNotFoundError).accumulating
      }
    }
  }

  private def verifyBlockchain(blocks: List[Block]) = {
    countBlocks() mustEqual blocks.size
    blocks.foreach { block =>
      val dbBlock = dataHandler.getBy(block.hash).get
      dbBlock.previousBlockhash mustEqual block.previousBlockhash
      dbBlock.nextBlockhash mustEqual block.nextBlockhash
    }
  }

  private def countBlocks() = {
    database.withConnection { implicit conn =>
      _root_.anorm.SQL("""SELECT COUNT(*) FROM blocks""").as(_root_.anorm.SqlParser.scalar[Int].single)
    }
  }
}
