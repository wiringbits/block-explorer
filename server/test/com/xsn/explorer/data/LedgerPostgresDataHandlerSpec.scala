package com.xsn.explorer.data

import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.{PreviousBlockMissingError, RepeatedBlockHeightError}
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers.Converters._
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.helpers.LedgerHelper._
import com.xsn.explorer.models.{BlockExtractionMethod, PoSBlockRewards, PoWBlockRewards, TPoSBlockRewards}
import org.scalactic.{Bad, Good}
import org.scalatest.BeforeAndAfter

class LedgerPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  private val emptyFilterFactory = () => GolombCodedSet(1, 2, 3, List(new UnsignedByte(0.toByte)))
  private val reward = Some(getPoWReward(blockList.head))
  lazy val dataHandler = createLedgerDataHandler(database)

  before {
    clearDatabase()
  }

  "push" should {
    "store the blocks linearly" in {
      blockList.foreach { block =>
        val transactions = getTransactions(block)

        dataHandler.push(block.withTransactions(transactions), List.empty, emptyFilterFactory, reward) mustEqual Good(
          ()
        )
      }
    }

    "fail to store the first block if it is not the genesis one" in {
      blockList.drop(1).foreach { block =>
        val transactions = getTransactions(block)

        dataHandler
          .push(block.withTransactions(transactions), List.empty, emptyFilterFactory, reward) mustEqual Bad(
          PreviousBlockMissingError
        ).accumulating
      }
    }

    "succeed storing a repeated block by hash" in {
      val genesis = blockList(0)
      dataHandler.push(genesis.withTransactions(getTransactions(genesis)), List.empty, emptyFilterFactory, reward) mustEqual Good(
        ()
      )
      dataHandler.push(genesis.withTransactions(getTransactions(genesis)), List.empty, emptyFilterFactory, reward) mustEqual Good(
        ()
      )
    }

    "fail to store a repeated block by height" in {
      val genesis = blockList(0)
      dataHandler.push(genesis.withTransactions(getTransactions(genesis)), List.empty, emptyFilterFactory, reward) mustEqual Good(
        ()
      )

      val block = blockList(1).copy(previousBlockhash = None, height = genesis.height)
      dataHandler.push(block.withTransactions(getTransactions(block)), List.empty, emptyFilterFactory, reward) mustEqual Bad(
        RepeatedBlockHeightError
      ).accumulating
    }

    "store block without reward" in {
      val block = blockList.head

      dataHandler.push(block.withTransactions(getTransactions(block)), List.empty, emptyFilterFactory, None) mustEqual Good(
        ()
      )

      database.withConnection { implicit conn =>
        val reward = blockRewardPostgresDAO.getBy(block.hash)
        reward mustEqual None
      }
    }

    "store PoW block rewards" in {
      val block = blockList.head
      val powReward = getPoWReward(block)
      val powBlock = toPersistedBlock(block)
        .copy(extractionMethod = BlockExtractionMethod.ProofOfWork)
        .withTransactions(getTransactions(block))

      dataHandler.push(powBlock, List.empty, emptyFilterFactory, Some(powReward)) mustEqual Good(())

      database.withConnection { implicit conn =>
        val reward = blockRewardPostgresDAO.getBy(block.hash)
        reward match {
          case Some(r: PoWBlockRewards) => {
            r.reward.address mustEqual powReward.reward.address
            r.reward.value mustEqual powReward.reward.value
          }
          case _ => fail
        }
      }
    }

    "store PoS block rewards" in {
      val block = blockList.head
      val posReward = getPoSReward(block)
      val posBlock = toPersistedBlock(block)
        .copy(extractionMethod = BlockExtractionMethod.ProofOfStake)
        .withTransactions(getTransactions(block))

      dataHandler.push(posBlock, List.empty, emptyFilterFactory, Some(posReward)) mustEqual Good(())

      database.withConnection { implicit conn =>
        val reward = blockRewardPostgresDAO.getBy(block.hash)
        reward match {
          case Some(r: PoSBlockRewards) => {
            r.coinstake.address mustEqual posReward.coinstake.address
            r.coinstake.value mustEqual posReward.coinstake.value

            r.masternode.get.address mustEqual posReward.masternode.get.address
            r.masternode.get.value mustEqual posReward.masternode.get.value

            r.stakedAmount mustEqual posReward.stakedAmount
            r.stakedDuration mustEqual posReward.stakedDuration
          }
          case _ => fail
        }
      }
    }

    "store PoS block rewards without masternode" in {
      val block = blockList.head
      val posReward = getPoSReward(block).copy(masternode = None)
      val posBlock = toPersistedBlock(block)
        .copy(extractionMethod = BlockExtractionMethod.ProofOfStake)
        .withTransactions(getTransactions(block))

      dataHandler.push(posBlock, List.empty, emptyFilterFactory, Some(posReward)) mustEqual Good(())

      database.withConnection { implicit conn =>
        val reward = blockRewardPostgresDAO.getBy(block.hash)
        reward match {
          case Some(r: PoSBlockRewards) => {
            r.coinstake.address mustEqual posReward.coinstake.address
            r.coinstake.value mustEqual posReward.coinstake.value

            r.masternode mustBe None

            r.stakedAmount mustEqual posReward.stakedAmount
            r.stakedDuration mustEqual posReward.stakedDuration
          }
          case _ => fail
        }
      }
    }

    "store TPoS block rewards" in {
      val block = blockList.head
      val tposReward = getTPoSReward(block)
      val tposBlock = toPersistedBlock(block)
        .copy(extractionMethod = BlockExtractionMethod.TrustlessProofOfStake)
        .withTransactions(getTransactions(block))

      dataHandler.push(tposBlock, List.empty, emptyFilterFactory, Some(tposReward)) mustEqual Good(())

      database.withConnection { implicit conn =>
        val reward = blockRewardPostgresDAO.getBy(block.hash)
        reward match {
          case Some(r: TPoSBlockRewards) => {
            r.owner.address mustEqual tposReward.owner.address
            r.owner.value mustEqual tposReward.owner.value

            r.merchant.address mustEqual tposReward.merchant.address
            r.merchant.value mustEqual tposReward.merchant.value

            r.masternode.get.address mustEqual tposReward.masternode.get.address
            r.masternode.get.value mustEqual tposReward.masternode.get.value

            r.stakedAmount mustEqual tposReward.stakedAmount
            r.stakedDuration mustEqual tposReward.stakedDuration
          }
          case _ => fail
        }
      }
    }

    "store TPoS block rewards without masternode" in {
      val block = blockList.head
      val tposReward = getTPoSReward(block).copy(masternode = None)
      val tposBlock = toPersistedBlock(block)
        .copy(extractionMethod = BlockExtractionMethod.TrustlessProofOfStake)
        .withTransactions(getTransactions(block))

      dataHandler.push(tposBlock, List.empty, emptyFilterFactory, Some(tposReward)) mustEqual Good(())

      database.withConnection { implicit conn =>
        val reward = blockRewardPostgresDAO.getBy(block.hash)
        reward match {
          case Some(r: TPoSBlockRewards) => {
            r.owner.address mustEqual tposReward.owner.address
            r.owner.value mustEqual tposReward.owner.value

            r.merchant.address mustEqual tposReward.merchant.address
            r.merchant.value mustEqual tposReward.merchant.value

            r.masternode mustBe None

            r.stakedAmount mustEqual tposReward.stakedAmount
            r.stakedDuration mustEqual tposReward.stakedDuration
          }
          case _ => fail
        }
      }
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

        dataHandler.push(block.withTransactions(transactions), List.empty, emptyFilterFactory, reward) mustEqual Good(
          ()
        )
      }

      blockList.reverse.foreach { block =>
        dataHandler.pop().get.hash mustEqual block.hash
      }
    }
  }
}
