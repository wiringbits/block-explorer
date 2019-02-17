package com.xsn.explorer.data

import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.{PreviousBlockMissingError, RepeatedBlockHeightError}
import com.xsn.explorer.helpers.Converters._
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.helpers.LedgerHelper._
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfter

class LedgerPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  lazy val dataHandler = createLedgerDataHandler(database)

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
        case _: Throwable => ()
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
}
