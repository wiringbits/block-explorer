package com.xsn.explorer.processors

import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, BlockPostgresDAO, StatisticsPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, BlockPostgresDataHandler, DatabasePostgresSeeder, StatisticsPostgresDataHandler}
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.helpers.{BlockLoader, Executors, FileBasedXSNService}
import com.xsn.explorer.models.base._
import com.xsn.explorer.models.fields.BalanceField
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.services.TransactionService
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures

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

      whenReady(processor.newLatestBlock(block0.hash)) { result =>
        result.isBad mustEqual true
      }
    }

    "process first block" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")

      whenReady(processor.newLatestBlock(block1.hash)) { result =>
        result.isGood mustEqual true
      }
    }

    "process a new block" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block1, block2).map(dataHandler.upsert).foreach(_.isGood mustEqual true)

      whenReady(processor.newLatestBlock(block3.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block1, block2, block3)
        verifyBlockchain(blocks)
      }
    }

    "process a rechain" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block1, block2, block3).map(dataHandler.upsert).foreach(_.isGood mustEqual true)

      whenReady(processor.newLatestBlock(block2.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block1, block2)
        verifyBlockchain(blocks)
      }
    }

    "process a missing block" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block2, block3).map(dataHandler.upsert).foreach(_.isGood mustEqual true)

      whenReady(processor.newLatestBlock(block1.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block1, block2, block3)
        verifyBlockchain(blocks)
      }
    }

    "processing a repeated missing block doesn't affect the balance" in {
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val block3 = BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd")

      List(block1, block2, block3).map(dataHandler.upsert).foreach(_.isGood mustEqual true)

      whenReady(processor.newLatestBlock(block1.hash)) { result =>
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

      whenReady(processor.newLatestBlock(block.hash)) { result =>
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
          .map(processor.newLatestBlock)
          .foreach { whenReady(_) { _.isGood mustEqual true } }

      whenReady(processor.newLatestBlock(block1.hash)) { result =>
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
