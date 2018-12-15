package com.xsn.explorer.data

import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.dao.{BalancePostgresDAO, StatisticsPostgresDAO}
import com.xsn.explorer.data.anorm.{BalancePostgresDataHandler, StatisticsPostgresDataHandler}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.helpers.DataHelper
import com.xsn.explorer.models.{Address, Balance}

class StatisticsPostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new StatisticsPostgresDataHandler(database, new StatisticsPostgresDAO)
  lazy val balanceDataHandler = new BalancePostgresDataHandler(database, new BalancePostgresDAO(new FieldOrderingSQLInterpreter))

  "getStatistics" should {
    "succeed even if there is no data" in {
      val result = dataHandler.getStatistics()
      result.isGood mustEqual true
    }

    "exclude hidden_addresses from the circulating supply" in {
      val hiddenAddress = DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt")
      val circulatingSupply = dataHandler.getStatistics().get.circulatingSupply

      database.withConnection { implicit conn =>
        _root_.anorm.SQL(
          s"""
            |INSERT INTO hidden_addresses (address)
            |VALUES ('${hiddenAddress.string}')
          """.stripMargin
        ).execute()
      }

      val balance = Balance(hiddenAddress, received = BigDecimal(1000), spent = BigDecimal(500))
      balanceDataHandler.upsert(balance).isGood mustEqual true

      val result = dataHandler.getStatistics().get
      result.circulatingSupply mustEqual circulatingSupply
    }

    "exclude the burn address from the total supply" in {
      val burnAddress = Address.from(StatisticsPostgresDAO.BurnAddress).get

      val totalSupply = dataHandler.getStatistics().get.totalSupply

      val balance = Balance(burnAddress, received = BigDecimal(1000), spent = BigDecimal(500))
      balanceDataHandler.upsert(balance).isGood mustEqual true

      val result = dataHandler.getStatistics().get
      result.totalSupply mustEqual totalSupply
    }
  }
}
