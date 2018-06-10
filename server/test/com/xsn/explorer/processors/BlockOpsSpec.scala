package com.xsn.explorer.processors

import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, BlockPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.{BlockPostgresDataHandler, DatabasePostgresSeeder}
import com.xsn.explorer.data.async.{BlockFutureDataHandler, DatabaseFutureSeeder}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.helpers.{BlockLoader, Executors, FileBasedXSNService}
import org.scalactic.Good
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures

class BlockOpsSpec extends PostgresDataHandlerSpec with ScalaFutures with BeforeAndAfter {

  before {
    clearDatabase()
  }

  lazy val dataHandler = new BlockPostgresDataHandler(database, new BlockPostgresDAO)
  lazy val dataSeeder = new DatabasePostgresSeeder(
    database,
    new BlockPostgresDAO,
    new TransactionPostgresDAO,
    new BalancePostgresDAO(new FieldOrderingSQLInterpreter))

  lazy val xsnService = new FileBasedXSNService

  lazy val blockOps = new BlockOps(
    new DatabaseFutureSeeder(dataSeeder)(Executors.databaseEC),
    new BlockFutureDataHandler(dataHandler)(Executors.databaseEC))


  val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
  val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")

  "createBlock" should {
    "create a new block" in {
      whenReady(blockOps.createBlock(block1, List.empty)) { result =>
        result mustEqual Good(BlockOps.Result.BlockCreated)
      }
    }

    "replace a block if the height already exist" in {
      whenReady(blockOps.createBlock(block1, List.empty)) { _.isGood mustEqual true }

      whenReady(blockOps.createBlock(block2.copy(height = block1.height), List.empty)) { result =>
        result mustEqual Good(BlockOps.Result.BlockReplacedByHeight)
      }
    }
  }
}
