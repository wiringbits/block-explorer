package controllers

import com.alexitc.playsonify.PublicErrorRenderer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.helpers.{DataHelper, DummyXSNService}
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.AddressBalance
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{One, Or}
import play.api.inject.bind
import play.api.libs.json.JsValue
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
}
