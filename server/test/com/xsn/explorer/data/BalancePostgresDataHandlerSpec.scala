package com.xsn.explorer.data

import com.xsn.explorer.data.anorm.BalancePostgresDataHandler
import com.xsn.explorer.data.anorm.dao.BalancePostgresDAO
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.helpers.DataHelper
import com.xsn.explorer.models.Balance
import org.scalactic.Good

class BalancePostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new BalancePostgresDataHandler(database, new BalancePostgresDAO)

  "upsert" should {
    "create an empty balance" in {
      val address = DataHelper.createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F")
      val balance = Balance(address)

      val result = dataHandler.upsert(balance)
      result mustEqual Good(balance)
    }

    "set the available amount" in {
      val address = DataHelper.createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF")
      val balance = Balance(address, received = BigDecimal(10), spent = BigDecimal(5))

      val result = dataHandler.upsert(balance)
      result mustEqual Good(balance)
      database.withConnection { implicit conn =>
        val available = _root_.anorm
            .SQL(s"SELECT available FROM balances WHERE address = '${address.string}'")
            .as(_root_.anorm.SqlParser.get[BigDecimal]("available").single)

        available mustEqual balance.available
      }
    }

    "update an existing balance" in {
      val address = DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt")
      val initialBalance = Balance(address, received = BigDecimal(10), spent = BigDecimal(5))
      val patch = Balance(address, received = BigDecimal(10), spent = BigDecimal(10))
      val expected = combine(initialBalance, patch)

      dataHandler.upsert(initialBalance)

      val result = dataHandler.upsert(patch)
      result mustEqual Good(expected)
    }
  }

  private def combine(balances: Balance*): Balance = {
    balances.reduce { (a, b) =>
      Balance(a.address, a.received + b.received, a.spent + b.spent)
    }
  }
}
