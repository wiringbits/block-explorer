package com.xsn.explorer.data

import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.LedgerPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.{AggregatedAmountPostgresDAO, BalancePostgresDAO, BlockPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.{PreviousBlockMissingError, RepeatedBlockHeightError}
import com.xsn.explorer.helpers.{BlockLoader, TransactionLoader}
import com.xsn.explorer.models.Transaction
import com.xsn.explorer.models.rpc.Block
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfter

class LedgerPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  lazy val dataHandler = new LedgerPostgresDataHandler(
    database,
    new BlockPostgresDAO(new FieldOrderingSQLInterpreter),
    new TransactionPostgresDAO(new FieldOrderingSQLInterpreter),
    new BalancePostgresDAO(new FieldOrderingSQLInterpreter),
    new AggregatedAmountPostgresDAO)

  val blockList = List(
    BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34"),
    BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7"),
    BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8"),
    BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd"),
    BlockLoader.get("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32"),
    BlockLoader.get("00000267225f7dba55d9a3493740e7f0dde0f28a371d2c3b42e7676b5728d020")
  )

  before {
    clearDatabase()
  }

  "push" should {
    "store the blocks linearly" in {
      blockList.foreach { block =>
        val transactions = getTransactions(block)

        dataHandler.push(block, transactions) mustEqual Good(())
      }
    }

    "fail to store the first block if it is not the genesis one" in {
      blockList.drop(1).foreach { block =>
        val transactions = getTransactions(block)

        dataHandler.push(block, transactions) mustEqual Bad(PreviousBlockMissingError).accumulating
      }
    }

    "succeed storing a repeated block by hash" in {
      val genesis = blockList(0)
      dataHandler.push(genesis, getTransactions(genesis)) mustEqual Good(())
      dataHandler.push(genesis, getTransactions(genesis)) mustEqual Good(())
    }

    "fail to store a repeated block by height" in {
      val genesis = blockList(0)
      dataHandler.push(genesis, getTransactions(genesis)) mustEqual Good(())

      val block = blockList(1).copy(previousBlockhash = None, height = genesis.height)
      dataHandler.push(block, getTransactions(block)) mustEqual Bad(RepeatedBlockHeightError).accumulating
    }
  }

  "pop" should {
    "fail on an empty ledger" in {
      try {
        dataHandler.pop()
        fail()
      } catch {
        case _ => ()
      }
    }

    "pop the blocks in order" in {
      blockList.foreach { block =>
        val transactions = getTransactions(block)

        dataHandler.push(block, transactions) mustEqual Good(())
      }

      blockList.reverse.foreach { block =>
        dataHandler.pop().get.hash mustEqual block.hash
      }
    }
  }

  private def getTransactions(block: Block) = {
    block
        .transactions
        .map(_.string)
        .map(TransactionLoader.get)
        .map(Transaction.fromRPC)
  }
}
