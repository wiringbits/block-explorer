package com.xsn.explorer.data

import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.{PreviousBlockMissingError, RepeatedBlockHeightError}
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers.Converters._
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.helpers.LedgerHelper._
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfter

class LedgerPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  private val emptyFilterFactory = () => GolombCodedSet(1, 2, 3, List(new UnsignedByte(0.toByte)))

  lazy val dataHandler = createLedgerDataHandler(database)

  before {
    clearDatabase()
  }

  "push" should {
    "store the blocks linearly" in {
      blockList.foreach { block =>
        val transactions = getTransactions(block)

        dataHandler.push(block.withTransactions(transactions), List.empty, emptyFilterFactory) mustEqual Good(())
      }
    }

    "fail to store the first block if it is not the genesis one" in {
      blockList.drop(1).foreach { block =>
        val transactions = getTransactions(block)

        dataHandler
          .push(block.withTransactions(transactions), List.empty, emptyFilterFactory) mustEqual Bad(
          PreviousBlockMissingError
        ).accumulating
      }
    }

    "succeed storing a repeated block by hash" in {
      val genesis = blockList(0)
      dataHandler.push(genesis.withTransactions(getTransactions(genesis)), List.empty, emptyFilterFactory) mustEqual Good(
        ()
      )
      dataHandler.push(genesis.withTransactions(getTransactions(genesis)), List.empty, emptyFilterFactory) mustEqual Good(
        ()
      )
    }

    "fail to store a repeated block by height" in {
      val genesis = blockList(0)
      dataHandler.push(genesis.withTransactions(getTransactions(genesis)), List.empty, emptyFilterFactory) mustEqual Good(
        ()
      )

      val block = blockList(1).copy(previousBlockhash = None, height = genesis.height)
      dataHandler.push(block.withTransactions(getTransactions(block)), List.empty, emptyFilterFactory) mustEqual Bad(
        RepeatedBlockHeightError
      ).accumulating
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

        dataHandler.push(block.withTransactions(transactions), List.empty, emptyFilterFactory) mustEqual Good(())
      }

      blockList.reverse.foreach { block =>
        dataHandler.pop().get.hash mustEqual block.hash
      }
    }
  }
}
