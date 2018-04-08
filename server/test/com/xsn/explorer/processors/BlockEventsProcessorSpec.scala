package com.xsn.explorer.processors

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.anorm.dao.BlockPostgresDAO
import com.xsn.explorer.data.anorm.{BlockPostgresDataHandler, DatabasePostgresSeeder}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.helpers.{BlockLoader, FileBasedXSNService}
import com.xsn.explorer.models.rpc.Block
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class BlockEventsProcessorSpec extends PostgresDataHandlerSpec with ScalaFutures with BeforeAndAfter {

  lazy val dataHandler = new BlockPostgresDataHandler(database, new BlockPostgresDAO)
  lazy val dataSeeder = new DatabasePostgresSeeder(database, new BlockPostgresDAO)

  before {
    clearDatabase()
  }

  "newLatestBlock" should {
    "process first block" in {
      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")

      val xsnService = new FileBasedXSNService {
        override def getLatestBlock(): FutureApplicationResult[Block] = {
          Future.successful(Bad(BlockNotFoundError).accumulating)
        }
      }

      val processor = new BlockEventsProcessor(xsnService, dataSeeder, dataHandler)
      whenReady(processor.newLatestBlock(block0.hash)) { result =>
        result.isGood mustEqual true
      }
    }

    "process a new block" in {
      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")

      List(block0, block1).foreach(dataHandler.upsert)
      val xsnService = new FileBasedXSNService {
        override def getLatestBlock(): FutureApplicationResult[Block] = {
          val latest = block1.copy(nextBlockhash = None)
          Future.successful(Good(latest).accumulating)
        }
      }

      val processor = new BlockEventsProcessor(xsnService, dataSeeder, dataHandler)
      whenReady(processor.newLatestBlock(block2.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block0, block1, block2)
        verifyBlockchain(blocks)
      }
    }

    "process a rechain" in {
      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")

      List(block0, block1, block2).foreach(dataHandler.upsert)
      val xsnService = new FileBasedXSNService {
        override def getLatestBlock(): FutureApplicationResult[Block] = {
          val latest = block2.copy(nextBlockhash = None)
          Future.successful(Good(latest).accumulating)
        }
      }

      val processor = new BlockEventsProcessor(xsnService, dataSeeder, dataHandler)
      whenReady(processor.newLatestBlock(block1.hash)) { result =>
        result.isGood mustEqual true
        val blocks = List(block0, block1)
        verifyBlockchain(blocks)
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
  private def clearDatabase() = {
    database.withConnection { implicit conn =>
      _root_.anorm.SQL("""DELETE FROM blocks""").execute()
    }
  }

  private def countBlocks() = {
    database.withConnection { implicit conn =>
      _root_.anorm.SQL("""SELECT COUNT(*) FROM blocks""").as(_root_.anorm.SqlParser.scalar[Int].single)
    }
  }
}
