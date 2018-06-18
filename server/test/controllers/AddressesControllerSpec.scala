package controllers

import com.alexitc.playsonify.PublicErrorRenderer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.helpers.{DataHelper, DummyXSNService}
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.AddressBalance
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{Good, One, Or}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._

import scala.concurrent.Future

class AddressesControllerSpec extends MyAPISpec {

  import DataHelper._

  val addressEmpty = createAddressDetails(0, 0, List())
  val addressFilled = createAddressDetails(
    100,
    200,
    List(
      createTransactionId("0834641a7d30d8a2d2b451617599670445ee94ed7736e146c13be260c576c641"),
      createTransactionId("024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c")
    )
  )

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

  val customXSNService = new DummyXSNService {
    val map = Map(
      "Xi3sQfMQsy2CzMZTrnKW6HFGp1VqFThdLw" -> addressEmpty,
      "XnH3bC9NruJ4wnu4Dgi8F3wemmJtcxpKp6" -> addressFilled
    )

    override def getAddressBalance(address: Address): FutureApplicationResult[AddressBalance] = {
      val maybe = map.get(address.string).map(_.balance)
      val result = Or.from(maybe, One(AddressFormatError))
      Future.successful(result)
    }

    override def getTransactions(address: Address): FutureApplicationResult[List[TransactionId]] = {
      val maybe = map.get(address.string).map(_.transactions)
      val result = Or.from(maybe, One(AddressFormatError))
      Future.successful(result)
    }

    override def getUnspentOutputs(address: Address): FutureApplicationResult[JsValue] = {
      if (address == addressForUtxos) {
        val result = Good(Json.parse(utxosResponse))
        Future.successful(result)
      } else {
        super.getUnspentOutputs(address)
      }
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(customXSNService))
      .build()

  "GET /addresses/:address" should {
    def url(address: String) = s"/addresses/$address"

    "retrieve address information" in {
      val address = addressFilled
      val response = GET(url("XnH3bC9NruJ4wnu4Dgi8F3wemmJtcxpKp6"))

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "balance").as[BigDecimal] mustEqual address.balance.balance
      (json \ "received").as[BigDecimal] mustEqual address.balance.received
      (json \ "transactions").as[List[String]].size mustEqual address.transactions.size
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
}
