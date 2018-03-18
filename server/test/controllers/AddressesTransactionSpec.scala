package controllers

import com.alexitc.playsonify.PublicErrorRenderer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.helpers.DummyXSNService
import com.xsn.explorer.models._
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{One, Or}
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._

import scala.concurrent.Future


class AddressesTransactionSpec extends MyAPISpec {

  def addressDetails(balance: Int, received: Int, txCount: Int) = {
    AddressDetails(AddressBalance(BigDecimal(balance), BigDecimal(received)), txCount)
  }

  val addressEmpty = addressDetails(0, 0, 0)
  val addressFilled = addressDetails(100, 200, 25)

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

    override def getTransactionCount(address: Address): FutureApplicationResult[Int] = {
      val maybe = map.get(address.string).map(_.transactionCount)
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
      (json \ "transactionCount").as[Int] mustEqual address.transactionCount
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
