package controllers

import com.xsn.explorer.data.BalanceBlockingDataHandler
import com.xsn.explorer.helpers.{BalanceDummyDataHandler, DataHelper}
import com.xsn.explorer.models.persisted.Balance
import controllers.common.MyAPISpec
import play.api.inject.bind

class BalancesControllerSpec extends MyAPISpec {

  val balances = List(
    Balance(
      address = DataHelper.createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F"),
      received = BigDecimal("1000"),
      spent = BigDecimal("0")
    ),
    Balance(
      address = DataHelper.createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
      received = BigDecimal("1000"),
      spent = BigDecimal("100")
    ),
    Balance(
      address = DataHelper.createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt"),
      received = BigDecimal("10000"),
      spent = BigDecimal("1000")
    ),
    Balance(
      address = DataHelper.createAddress("XiHW7SR56UPHeXKwcpeVsE4nUfkHv5RqE3"),
      received = BigDecimal("1000"),
      spent = BigDecimal("500")
    )
  ).sortBy(_.available).reverse

  val dataHandler = new BalanceDummyDataHandler {}

  val application = guiceApplicationBuilder
    .overrides(bind[BalanceBlockingDataHandler].to(dataHandler))
    .build()

}
