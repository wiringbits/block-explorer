package com.xsn.explorer.data

import java.time.Instant
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, StatisticsPostgresDAO, TPoSContractDAO}
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, StatisticsPostgresDataHandler}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers.DataGenerator.randomTransaction
import com.xsn.explorer.helpers.DataHandlerObjects.createLedgerDataHandler
import com.xsn.explorer.helpers.{BlockLoader, DataGenerator, DataHelper}
import com.xsn.explorer.models.{
  AddressesReward,
  BlockExtractionMethod,
  BlockReward,
  BlockRewards,
  PoSBlockRewards,
  TPoSBlockRewards
}
import com.xsn.explorer.models.persisted.Balance
import com.xsn.explorer.models.values.{Address, Height}
import org.scalactic.Good
import org.scalatest.BeforeAndAfter

import scala.concurrent.duration._

@com.github.ghik.silencer.silent
class StatisticsPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  val secondsInOneDay = 24 * 60 * 60

  lazy val dataHandler = new StatisticsPostgresDataHandler(
    database,
    new StatisticsPostgresDAO,
    new TPoSContractDAO()
  )
  lazy val ledgerDataHandler = createLedgerDataHandler(database)

  lazy val balanceDataHandler =
    new BalancePostgresDataHandler(
      database,
      new BalancePostgresDAO(new FieldOrderingSQLInterpreter)
    )

  before {
    clearDatabase()
  }

  "getStatistics" should {
    "succeed even if there is no data" in {
      val result = dataHandler.getStatistics()
      result.isGood mustEqual true
    }

    "exclude hidden_addresses from the circulating supply" in {
      val hiddenAddress =
        DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt")
      val circulatingSupply =
        dataHandler.getStatistics().get.circulatingSupply.getOrElse(0)

      database.withConnection { implicit conn =>
        _root_.anorm
          .SQL(
            s"""
            |INSERT INTO hidden_addresses (address)
            |VALUES ('${hiddenAddress.string}')
          """.stripMargin
          )
          .execute()
      }

      val balance = Balance(
        hiddenAddress,
        received = BigDecimal(1000),
        spent = BigDecimal(500)
      )
      setAvailableCoins(balance.available)
      balanceDataHandler.upsert(balance).isGood mustEqual true

      val result = dataHandler.getStatistics().get
      result.circulatingSupply.getOrElse(0) mustEqual circulatingSupply
    }

    "exclude the burn address from the total supply" in {
      val burnAddress = Address.from(StatisticsPostgresDAO.BurnAddress).get

      val totalSupply = dataHandler.getStatistics().get.totalSupply.getOrElse(0)

      val balance = Balance(
        burnAddress,
        received = BigDecimal(1000),
        spent = BigDecimal(500)
      )
      setAvailableCoins(balance.available)
      balanceDataHandler.upsert(balance).isGood mustEqual true

      val result = dataHandler.getStatistics().get
      result.totalSupply.getOrElse(0) mustEqual totalSupply
    }
  }

  "getRewardsSummary" should {
    "get the correct rewards summary when requesting as much blocks as there are stored" in {
      pushPoSBlock(
        "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32",
        0,
        100,
        1500,
        1 * secondsInOneDay
      )
      pushTPoSBlock(
        "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34",
        1,
        200,
        9999,
        2 * secondsInOneDay
      )
      pushTPoSBlock(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0",
        2,
        300,
        1234,
        3 * secondsInOneDay
      )
      pushPoSBlock(
        "000001ff95f22b8d82db14a5c5e9f725e8239e548be43c668766e7ddaee81924",
        3,
        400,
        4321,
        4 * secondsInOneDay
      )

      dataHandler.getRewardsSummary(4) match {
        case Good(s) =>
          areAlmostEqual(
            s.averageReward,
            (100 + 200 + 300 + 400) / 4.0
          ) mustBe true
          areAlmostEqual(
            s.averageInput,
            (1500 + 9999 + 1234 + 4321) / 4.0
          ) mustBe true
          areAlmostEqual(s.medianInput, (1500 + 4321) / 2.0) mustBe true
          areAlmostEqual(s.averagePoSInput, (1500 + 4321) / 2.0) mustBe true
          areAlmostEqual(s.averageTPoSInput, (9999 + 1234) / 2.0) mustBe true
          areAlmostEqual(
            s.medianWaitTime,
            ((2 + 3) / 2.0) * secondsInOneDay
          ) mustBe true
          areAlmostEqual(
            s.averageWaitTime,
            ((1 + 2 + 3 + 4) / 4.0) * secondsInOneDay
          ) mustBe true
        case _ => fail
      }
    }

    "get the correct rewards summary when requesting less blocks than there are stored" in {
      pushPoSBlock(
        "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32",
        0,
        100,
        1500,
        1 * secondsInOneDay
      )
      pushPoSBlock(
        "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34",
        1,
        200,
        9999,
        2 * secondsInOneDay
      )
      pushTPoSBlock(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0",
        2,
        300,
        1234,
        3 * secondsInOneDay
      )
      pushPoSBlock(
        "000001ff95f22b8d82db14a5c5e9f725e8239e548be43c668766e7ddaee81924",
        3,
        400,
        4321,
        4 * secondsInOneDay
      )

      dataHandler.getRewardsSummary(2) match {
        case Good(s) =>
          areAlmostEqual(s.averageReward, (300 + 400) / 2.0) mustBe true
          areAlmostEqual(s.averageInput, (1234 + 4321) / 2.0) mustBe true
          areAlmostEqual(s.medianInput, (1234 + 4321) / 2.0) mustBe true
          areAlmostEqual(s.averagePoSInput, 4321) mustBe true
          areAlmostEqual(s.averageTPoSInput, 1234) mustBe true
          areAlmostEqual(
            s.medianWaitTime,
            ((3 + 4) / 2.0) * secondsInOneDay
          ) mustBe true
          areAlmostEqual(
            s.averageWaitTime,
            ((3 + 4) / 2.0) * secondsInOneDay
          ) mustBe true
        case _ => fail
      }
    }

    "get the correct rewards summary when requesting more blocks than there are stored" in {
      pushTPoSBlock(
        "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32",
        0,
        100,
        1500,
        1 * secondsInOneDay
      )
      pushPoSBlock(
        "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34",
        1,
        200,
        9999,
        2 * secondsInOneDay
      )
      pushTPoSBlock(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0",
        2,
        300,
        1234,
        3 * secondsInOneDay
      )
      pushTPoSBlock(
        "000001ff95f22b8d82db14a5c5e9f725e8239e548be43c668766e7ddaee81924",
        3,
        400,
        4321,
        4 * secondsInOneDay
      )

      dataHandler.getRewardsSummary(999) match {
        case Good(s) =>
          areAlmostEqual(
            s.averageReward,
            (100 + 200 + 300 + 400) / 4.0
          ) mustBe true
          areAlmostEqual(
            s.averageInput,
            (1500 + 9999 + 1234 + 4321) / 4.0
          ) mustBe true
          areAlmostEqual(s.medianInput, (1500 + 4321) / 2.0) mustBe true
          areAlmostEqual(s.averagePoSInput, 9999) mustBe true
          areAlmostEqual(
            s.averageTPoSInput,
            (1500 + 1234 + 4321) / 3.0
          ) mustBe true
          areAlmostEqual(
            s.medianWaitTime,
            ((2 + 3) / 2.0) * secondsInOneDay
          ) mustBe true
          areAlmostEqual(
            s.averageWaitTime,
            ((1 + 2 + 3 + 4) / 4.0) * secondsInOneDay
          ) mustBe true
        case _ => fail
      }
    }

    "get the correct rewards summary when requesting only TPoS blocks" in {
      pushTPoSBlock(
        "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32",
        0,
        100,
        1500,
        1 * secondsInOneDay
      )
      pushTPoSBlock(
        "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34",
        1,
        200,
        9999,
        2 * secondsInOneDay
      )

      dataHandler.getRewardsSummary(2) match {
        case Good(s) =>
          areAlmostEqual(s.averageReward, (100 + 200) / 2.0) mustBe true
          areAlmostEqual(s.averageInput, (1500 + 9999) / 2.0) mustBe true
          areAlmostEqual(s.medianInput, (1500 + 9999) / 2.0) mustBe true
          areAlmostEqual(s.averagePoSInput, 0) mustBe true
          areAlmostEqual(s.averageTPoSInput, (1500 + 9999) / 2.0) mustBe true
          areAlmostEqual(
            s.medianWaitTime,
            ((1 + 2) / 2.0) * secondsInOneDay
          ) mustBe true
          areAlmostEqual(
            s.averageWaitTime,
            ((1 + 2) / 2.0) * secondsInOneDay
          ) mustBe true
        case _ => fail
      }
    }

    "get the correct rewards summary when requesting only PoS blocks" in {
      pushPoSBlock(
        "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32",
        0,
        100,
        1500,
        1 * secondsInOneDay
      )
      pushPoSBlock(
        "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34",
        1,
        200,
        9999,
        2 * secondsInOneDay
      )

      dataHandler.getRewardsSummary(2) match {
        case Good(s) =>
          areAlmostEqual(s.averageReward, (100 + 200) / 2.0) mustBe true
          areAlmostEqual(s.averageInput, (1500 + 9999) / 2.0) mustBe true
          areAlmostEqual(s.medianInput, (1500 + 9999) / 2.0) mustBe true
          areAlmostEqual(s.averagePoSInput, (1500 + 9999) / 2.0) mustBe true
          areAlmostEqual(s.averageTPoSInput, 0) mustBe true
          areAlmostEqual(
            s.medianWaitTime,
            ((1 + 2) / 2.0) * secondsInOneDay
          ) mustBe true
          areAlmostEqual(
            s.averageWaitTime,
            ((1 + 2) / 2.0) * secondsInOneDay
          ) mustBe true
        case _ => fail
      }
    }

    "get the correct rewards summary when there are no blocks" in {
      dataHandler.getRewardsSummary(2) match {
        case Good(s) =>
          areAlmostEqual(s.averageReward, 0) mustBe true
          areAlmostEqual(s.averageInput, 0) mustBe true
          areAlmostEqual(s.medianInput, 0) mustBe true
          areAlmostEqual(s.averagePoSInput, 0) mustBe true
          areAlmostEqual(s.averageTPoSInput, 0) mustBe true
          areAlmostEqual(s.medianWaitTime, 0) mustBe true
          areAlmostEqual(s.averageWaitTime, 0) mustBe true
        case _ => fail
      }
    }

  }

  "getRewardedAddressesCount" should {
    "get the count of rewarded addresses in the last 72 hours" in {
      val address1 = DataGenerator.randomAddress
      val address2 = DataGenerator.randomAddress
      val address3 = DataGenerator.randomAddress
      val address4 = DataGenerator.randomAddress

      pushTPoSBlock(
        "00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32",
        0,
        200,
        9999,
        2 * secondsInOneDay,
        rewardOwnerAddress = address1,
        rewarMerchantdAddress = address4,
        time = Instant.now
      )

      pushTPoSBlock(
        "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34",
        1,
        200,
        9999,
        2 * secondsInOneDay,
        rewardOwnerAddress = address2,
        rewarMerchantdAddress = address4,
        time = Instant.now
      )

      pushTPoSBlock(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0",
        2,
        200,
        9999,
        2 * secondsInOneDay,
        rewardOwnerAddress = address3,
        rewarMerchantdAddress = address4,
        time = Instant.now.minusSeconds(73.hours.toSeconds)
      )

      val startDate = Instant.now.minusSeconds(72.hours.toSeconds)
      val expected = AddressesReward(3, BigDecimal(400))
      dataHandler.getRewardedAddresses(startDate) match {
        case Good(result) =>
          result mustBe expected
        case _ => fail
      }
    }

    "return 0 when there are no rewards" in {
      val startDate = Instant.now.minusSeconds(72.hours.toSeconds)
      val expected = AddressesReward(0, BigDecimal(0))
      dataHandler.getRewardedAddresses(startDate) match {
        case Good(result) =>
          result mustBe expected
        case _ => fail
      }
    }
  }

  private def setAvailableCoins(total: BigDecimal) = {
    database.withConnection { implicit conn =>
      _root_.anorm
        .SQL(
          s"""
           |UPDATE aggregated_amounts
           |SET value = value + $total
           |WHERE name = 'available_coins'
          """.stripMargin
        )
        .executeUpdate()
    }
  }

  private def pushBlock(
      blockhash: String,
      blockHeight: Int,
      extractionMethod: BlockExtractionMethod,
      reward: BlockRewards,
      time: Instant
  ) = {
    val emptyFilterFactory = () => GolombCodedSet(1, 2, 3, List(new UnsignedByte(0.toByte)))

    val block = BlockLoader
      .get(blockhash)
      .copy(
        previousBlockhash = None,
        nextBlockhash = None,
        height = Height(blockHeight),
        extractionMethod = extractionMethod,
        time = time.getEpochSecond
      )

    val tx = randomTransaction(blockhash = block.hash, utxos = List.empty)
    val blockWithTransactions = block.withTransactions(List(tx))
    ledgerDataHandler
      .push(blockWithTransactions, List.empty, emptyFilterFactory, Some(reward))
      .isGood mustEqual true
  }

  private def pushPoSBlock(
      blockhash: String,
      blockHeight: Int,
      rewardValue: BigDecimal,
      rewardStakedValue: BigDecimal,
      rewardStakeWaitTime: Long,
      rewardAddress: Address = DataGenerator.randomAddress,
      time: Instant = Instant.now
  ) = {
    val reward = PoSBlockRewards(
      BlockReward(rewardAddress, rewardValue),
      None,
      rewardStakedValue,
      rewardStakeWaitTime
    )

    pushBlock(
      blockhash,
      blockHeight,
      BlockExtractionMethod.ProofOfStake,
      reward,
      time
    )
  }

  private def pushTPoSBlock(
      blockhash: String,
      blockHeight: Int,
      rewardValue: BigDecimal,
      rewardStakedValue: BigDecimal,
      rewardStakeWaitTime: Long,
      rewardOwnerAddress: Address = DataGenerator.randomAddress,
      rewarMerchantdAddress: Address = DataGenerator.randomAddress,
      time: Instant = Instant.now
  ) = {
    val reward = TPoSBlockRewards(
      BlockReward(rewardOwnerAddress, rewardValue * 0.9),
      BlockReward(rewarMerchantdAddress, rewardValue * 0.1),
      None,
      rewardStakedValue,
      rewardStakeWaitTime
    )

    pushBlock(
      blockhash,
      blockHeight,
      BlockExtractionMethod.TrustlessProofOfStake,
      reward,
      time
    )
  }

  private def areAlmostEqual(
      n1: BigDecimal,
      n2: BigDecimal,
      epsilon: Double = 1e-6
  ) = {
    n1 === (n2 +- epsilon)
  }
}
