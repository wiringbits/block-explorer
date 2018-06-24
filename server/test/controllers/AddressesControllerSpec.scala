package controllers

import com.alexitc.playsonify.PublicErrorRenderer
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.alexitc.playsonify.models.{Count, FieldOrdering, PaginatedQuery, PaginatedResult}
import com.xsn.explorer.data.{BalanceBlockingDataHandler, TransactionBlockingDataHandler}
import com.xsn.explorer.helpers.{BalanceDummyDataHandler, DataHelper, DummyXSNService, TransactionDummyDataHandler}
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.Good
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._

import scala.concurrent.Future

class AddressesControllerSpec extends MyAPISpec {

  import DataHelper._

  val addressWithBalance = createAddress("XeNEPsgeWqNbrEGEN5vqv4wYcC3qQrqNyp")
  val addressBalance = Balance(addressWithBalance, spent = 100, received = 200)

  val addressForUtxos = DataHelper.createAddress("XeNEPsgeWqNbrEGEN5vqv4wYcC3qQrqNyp")
  val utxosResponse =
    """
      |[
      |    {
      |        "address": "XeNEPsgeWqNbrEGEN5vqv4wYcC3qQrqNyp",
      |        "height": 22451,
      |        "outputIndex": 0,
      |        "satoshis": 1500000000000,
      |        "script": "76a914285b6f1ccacea0059ff5393cb4eb2f0569e2b3e988ac",
      |        "txid": "ea837f2011974b6a1a2fa077dc33684932c514a4ec6febc10e1a19ebe1336539"
      |    },
      |    {
      |        "address": "XeNEPsgeWqNbrEGEN5vqv4wYcC3qQrqNyp",
      |        "height": 25093,
      |        "outputIndex": 3,
      |        "satoshis": 2250000000,
      |        "script": "76a914285b6f1ccacea0059ff5393cb4eb2f0569e2b3e988ac",
      |        "txid": "96a06b802d1c15818a42aa9b46dd2e236cde746000d35f74d3eb940ab9d5694d"
      |    }
      |]
    """.stripMargin

  val addressForTransactions = createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW86F")
  val addressTransaction = TransactionWithValues(
    createTransactionId("92c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
    createBlockhash("ad22f0dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
    12312312L,
    Size(1000),
    sent = 50,
    received = 200)


  val customXSNService = new DummyXSNService {

    override def getUnspentOutputs(address: Address): FutureApplicationResult[JsValue] = {
      if (address == addressForUtxos) {
        val result = Good(Json.parse(utxosResponse))
        Future.successful(result)
      } else {
        super.getUnspentOutputs(address)
      }
    }
  }

  private val customTransactionDataHandler = new TransactionDummyDataHandler {

    override def getBy(address: Address, paginatedQuery: PaginatedQuery, ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = {
      if (address == addressForTransactions) {
        Good(PaginatedResult(paginatedQuery.offset, paginatedQuery.limit, Count(1), List(addressTransaction)))
      } else {
        Good(PaginatedResult(paginatedQuery.offset, paginatedQuery.limit, Count(0), List.empty))
      }
    }
  }

  private val customBalanceDataHandler = new BalanceDummyDataHandler {
    override def getBy(address: Address): ApplicationResult[Balance] = {
      if (address == addressWithBalance) {
        Good(addressBalance)
      } else {
        Good(Balance(address))
      }
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(customXSNService))
      .overrides(bind[TransactionBlockingDataHandler].to(customTransactionDataHandler))
      .overrides(bind[BalanceBlockingDataHandler].to(customBalanceDataHandler))
      .build()

  "GET /addresses/:address" should {
    def url(address: String) = s"/addresses/$address"

    "retrieve address information" in {
      val address = addressWithBalance
      val response = GET(url(address.string))

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "spent").as[BigDecimal] mustEqual addressBalance.spent
      (json \ "received").as[BigDecimal] mustEqual addressBalance.received
    }

    "fail on bad address format" in {
      val address = "XnH3bC9NruJ4wnu4Dgi8F3wemmJtcxpKp"
      val response = GET(url(address))

      status(response) mustEqual BAD_REQUEST
      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "address"
    }
  }

  "GET /addresses/:address/utxos" should {
    def url(address: String) = s"/addresses/$address/utxos"

    "return an array with the result" in {
      val response = GET(url(addressForUtxos.string))

      status(response) mustEqual OK
      contentAsJson(response) mustEqual Json.parse(utxosResponse)
    }
  }

  "GET /addresses/:address/transactions" should {
    def url(address: String, offset: Int, limit: Int) = s"/addresses/$address/transactions?offset=$offset&limit=$limit"

    "return the transactions where the address was involved" in {
      val offset = 0
      val limit = 5
      val response = GET(url(addressForTransactions.string, offset, limit))

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "offset").as[Int] mustEqual offset
      (json \ "limit").as[Int] mustEqual limit
      (json \ "total").as[Int] mustEqual 1

      val data = (json \ "data").as[List[JsValue]]
      data.size mustEqual 1

      val item = data.head
      (item \ "id").as[String] mustEqual addressTransaction.id.string
      (item \ "blockhash").as[String] mustEqual addressTransaction.blockhash.string
      (item \ "time").as[Long] mustEqual addressTransaction.time
      (item \ "size").as[Int] mustEqual addressTransaction.size.int
      (item \ "sent").as[BigDecimal] mustEqual addressTransaction.sent
      (item \ "received").as[BigDecimal] mustEqual addressTransaction.received
    }
  }
}
