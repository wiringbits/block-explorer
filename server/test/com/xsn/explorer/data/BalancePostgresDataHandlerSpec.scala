package com.xsn.explorer.data

import com.xsn.explorer.data.anorm.BalancePostgresDataHandler
import com.xsn.explorer.data.anorm.dao.BalancePostgresDAO
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.helpers.DataHelper
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.base._
import com.xsn.explorer.models.fields.BalanceField
import org.scalactic.Good

class BalancePostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new BalancePostgresDataHandler(database, new BalancePostgresDAO(new FieldOrderingSQLInterpreter))

  val defaultOrdering = FieldOrdering(BalanceField.Available, OrderingCondition.DescendingOrder)

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

  "get" should {

    val balances = List(
      Balance(
        address = DataHelper.createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F"),
        received = BigDecimal("1000"),
        spent = BigDecimal("0")),

      Balance(
        address = DataHelper.createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
        received = BigDecimal("1000"),
        spent = BigDecimal("100")),

      Balance(
        address = DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt"),
        received = BigDecimal("10000"),
        spent = BigDecimal("1000")),

      Balance(
        address = DataHelper.createAddress("XiHW7SR56UPHeXKwcpeVsE4nUfkHv5RqE3"),
        received = BigDecimal("1000"),
        spent = BigDecimal("500"))
    ).sortBy(_.available).reverse

    def prepare() = {
      clearDatabase()

      balances
          .map(dataHandler.upsert)
          .foreach(_.isGood mustEqual true)
    }

    "return the first 3 richest addresses" in {
      prepare()
      val query = PaginatedQuery(Offset(0), Limit(3))
      val expected = balances.take(3)

      val result = dataHandler.get(query, defaultOrdering)
      result.map(_.data) mustEqual Good(expected)
    }

    "skip the first richest address" in {
      prepare()
      val query = PaginatedQuery(Offset(1), Limit(3))
      val expected = balances.drop(1).take(3)

      val result = dataHandler.get(query, defaultOrdering)
      result.map(_.data) mustEqual Good(expected)
    }
  }

  private def combine(balances: Balance*): Balance = {
    balances.reduce { (a, b) =>
      Balance(a.address, a.received + b.received, a.spent + b.spent)
    }
  }
}
