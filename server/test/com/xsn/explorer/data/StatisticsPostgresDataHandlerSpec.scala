package com.xsn.explorer.data

import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, StatisticsPostgresDAO}
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, StatisticsPostgresDataHandler}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers.DataGenerator.randomTransaction
import com.xsn.explorer.helpers.DataHandlerObjects.createLedgerDataHandler
import com.xsn.explorer.helpers.{BlockLoader, DataGenerator, DataHelper}
import com.xsn.explorer.models.{BlockReward, PoSBlockRewards}
import com.xsn.explorer.models.persisted.Balance
import com.xsn.explorer.models.values.{Address, Height}
import org.scalactic.Good
import org.scalatest.OptionValues._

class StatisticsPostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new StatisticsPostgresDataHandler(database, new StatisticsPostgresDAO)
  lazy val ledgerDataHandler = createLedgerDataHandler(database)
  lazy val balanceDataHandler =
    new BalancePostgresDataHandler(database, new BalancePostgresDAO(new FieldOrderingSQLInterpreter))

  "getStatistics" should {
    "succeed even if there is no data" in {
      val result = dataHandler.getStatistics()
      result.isGood mustEqual true
    }

    "exclude hidden_addresses from the circulating supply" in {
      val hiddenAddress = DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt")
      val circulatingSupply = dataHandler.getStatistics().get.circulatingSupply.getOrElse(0)

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

      val balance = Balance(hiddenAddress, received = BigDecimal(1000), spent = BigDecimal(500))
      setAvailableCoins(balance.available)
      balanceDataHandler.upsert(balance).isGood mustEqual true

      val result = dataHandler.getStatistics().get
      result.circulatingSupply.value mustEqual circulatingSupply
    }

    "exclude the burn address from the total supply" in {
      val burnAddress = Address.from(StatisticsPostgresDAO.BurnAddress).get

      val totalSupply = dataHandler.getStatistics().get.totalSupply.getOrElse(0)

      val balance = Balance(burnAddress, received = BigDecimal(1000), spent = BigDecimal(500))
      setAvailableCoins(balance.available)
      balanceDataHandler.upsert(balance).isGood mustEqual true

      val result = dataHandler.getStatistics().get
      result.totalSupply.value mustEqual totalSupply
    }
  }

  "getRewardsSummary" should {
    "get the correct rewards summary when requesting as much blocks as there are stored" in {
      val secondsInOneDay = 24 * 60 * 60

      pushBlock("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32", 0, 100, 1500, 1 * secondsInOneDay)
      pushBlock("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34", 1, 200, 9999, 2 * secondsInOneDay)
      pushBlock("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0", 2, 300, 1234, 3 * secondsInOneDay)
      pushBlock("000001ff95f22b8d82db14a5c5e9f725e8239e548be43c668766e7ddaee81924", 3, 400, 4321, 4 * secondsInOneDay)

      dataHandler.getRewardsSummary(4) match {
        case Good(s) =>
          s.averageReward mustEqual ((100 + 200 + 300 + 400) / 4.0)
          s.averageInput mustEqual ((1500 + 9999 + 1234 + 4321) / 4.0)
          s.medianWaitTime mustEqual (((2 + 3) / 2.0) * secondsInOneDay)
        case _ => fail
      }
    }

    "get the correct rewards summary when requesting less blocks than there are stored" in {
      val secondsInOneDay = 24 * 60 * 60

      pushBlock("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32", 0, 100, 1500, 1 * secondsInOneDay)
      pushBlock("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34", 1, 200, 9999, 2 * secondsInOneDay)
      pushBlock("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0", 2, 300, 1234, 3 * secondsInOneDay)
      pushBlock("000001ff95f22b8d82db14a5c5e9f725e8239e548be43c668766e7ddaee81924", 3, 400, 4321, 4 * secondsInOneDay)

      dataHandler.getRewardsSummary(2) match {
        case Good(s) =>
          s.averageReward mustEqual ((300 + 400) / 2.0)
          s.averageInput mustEqual ((1234 + 4321) / 2.0)
          s.medianWaitTime mustEqual (((3 + 4) / 2.0) * secondsInOneDay)
        case _ => fail
      }
    }

    "get the correct rewards summary when requesting more blocks than there are stored" in {
      val secondsInOneDay = 24 * 60 * 60

      pushBlock("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32", 0, 100, 1500, 1 * secondsInOneDay)
      pushBlock("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34", 1, 200, 9999, 2 * secondsInOneDay)
      pushBlock("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0", 2, 300, 1234, 3 * secondsInOneDay)
      pushBlock("000001ff95f22b8d82db14a5c5e9f725e8239e548be43c668766e7ddaee81924", 3, 400, 4321, 4 * secondsInOneDay)

      dataHandler.getRewardsSummary(999) match {
        case Good(s) =>
          s.averageReward mustEqual ((100 + 200 + 300 + 400) / 4.0)
          s.averageInput mustEqual ((1500 + 9999 + 1234 + 4321) / 4.0)
          s.medianWaitTime mustEqual (((2 + 3) / 2.0) * secondsInOneDay)
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
      rewardValue: BigDecimal,
      rewardStakedValue: BigDecimal,
      rewardStakeWaitTime: Long
  ) = {
    val emptyFilterFactory = () => GolombCodedSet(1, 2, 3, List(new UnsignedByte(0.toByte)))

    val block = BlockLoader
      .get(blockhash)
      .copy(previousBlockhash = None, nextBlockhash = None, height = Height(blockHeight))

    val baseTime = java.lang.System.currentTimeMillis / 1000
    val rewardTime = baseTime + rewardStakeWaitTime

    val stakedTransaction = {
      val tx = randomTransaction(blockhash = block.hash, utxos = List.empty, time = baseTime)
      val outputs = DataGenerator.randomOutputs(1).map(_.copy(txid = tx.id, value = rewardStakedValue))
      tx.copy(outputs = outputs)
    }

    val rewardTransaction = {
      randomTransaction(blockhash = block.hash, utxos = stakedTransaction.outputs, time = rewardTime)
    }

    val reward = PoSBlockRewards(
      BlockReward(DataGenerator.randomAddress, rewardValue),
      None,
      rewardStakedValue,
      rewardStakeWaitTime
    )
    val blockWithTransactions = block.withTransactions(List(stakedTransaction, rewardTransaction))

    ledgerDataHandler.push(blockWithTransactions, List.empty, emptyFilterFactory, Some(reward)).isGood mustEqual true
  }
}
