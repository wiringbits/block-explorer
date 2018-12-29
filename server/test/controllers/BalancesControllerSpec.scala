package controllers

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.FieldOrdering
import com.alexitc.playsonify.models.pagination._
import com.xsn.explorer.data.BalanceBlockingDataHandler
import com.xsn.explorer.helpers.{BalanceDummyDataHandler, DataHelper}
import com.xsn.explorer.models.Balance
import com.xsn.explorer.models.fields.BalanceField
import controllers.common.MyAPISpec
import org.scalactic.Good
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._

class BalancesControllerSpec extends MyAPISpec {

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
      spent = BigDecimal("500")))
      .sortBy(_.available)
      .reverse

  val dataHandler = new BalanceDummyDataHandler {

    override def getNonZeroBalances(query: PaginatedQuery, ordering: FieldOrdering[BalanceField]): ApplicationResult[PaginatedResult[Balance]] = {
      val filtered = balances.filter(_.available > 0)
      val list = filtered.drop(query.offset.int).take(query.limit.int)
      val result = PaginatedResult(
        offset = query.offset,
        limit = query.limit,
        total = Count(filtered.size),
        data = list)

      Good(result)
    }
  }

  val application = guiceApplicationBuilder
      .overrides(bind[BalanceBlockingDataHandler].to(dataHandler))
      .build()

  "GET /balances" should {
    "get the richest addresses" in {
      val query = PaginatedQuery(Offset(1), Limit(2))
      val expected1 = balances(1)
      val expected2 = balances(2)
      val response = GET(s"/balances?offset=${query.offset.int}&limit=${query.limit.int}")

      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "total").as[Int] mustEqual balances.size
      (json \ "offset").as[Int] mustEqual query.offset.int
      (json \ "limit").as[Int] mustEqual query.limit.int

      val list = (json \ "data").as[List[JsValue]]
      list.size mustEqual 2

      (list(0) \ "address").as[String] mustEqual expected1.address.string
      (list(0) \ "available").as[BigDecimal] mustEqual expected1.available
      (list(0) \ "received").as[BigDecimal] mustEqual expected1.received
      (list(0) \ "spent").as[BigDecimal] mustEqual expected1.spent

      (list(1) \ "address").as[String] mustEqual expected2.address.string
      (list(1) \ "available").as[BigDecimal] mustEqual expected2.available
      (list(1) \ "received").as[BigDecimal] mustEqual expected2.received
      (list(1) \ "spent").as[BigDecimal] mustEqual expected2.spent
    }
  }
}
