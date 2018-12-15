package controllers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.play.PublicErrorRenderer
import com.xsn.explorer.errors.MasternodeNotFoundError
import com.xsn.explorer.helpers.DummyXSNService
import com.xsn.explorer.models.rpc.Masternode
import com.xsn.explorer.models.{Address, IPAddress, TransactionId}
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{Bad, Good}
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._

import scala.concurrent.Future

class MasternodesControllerSpec extends MyAPISpec {

  val masternodes = List(
    Masternode(
      txid = TransactionId.from("c3efb8b60bda863a3a963d340901dc2b870e6ea51a34276a8f306d47ffb94f01").get,
      ip = "45.77.136.212:62583",
      protocol = "70208",
      status = "WATCHDOG_EXPIRED",
      activeSeconds = 513323,
      lastSeen = 1524349009,
      payee = Address.from("XqdmM7rop8Sdgn8UjyNh3Povc3rhNSXYw2").get),

    Masternode(
      txid = TransactionId.from("b02f99d87194c9400ab147c070bf621770684906dedfbbe9ba5f3a35c26b8d01").get,
      ip = "45.32.148.13:62583",
      protocol = "70208",
      status = "ENABLED",
      activeSeconds = 777344,
      lastSeen = 1524349028,
      payee = Address.from("XdNDRAiMUC9KiVRzhCTg9w44jQRdCpCRe3").get)
  )

  val masternode = masternodes.last

  val xsnService = new DummyXSNService {
    override def getMasternodes(): FutureApplicationResult[List[Masternode]] = {
      Future.successful(Good(masternodes))
    }

    override def getMasternode(ipAddress: IPAddress): FutureApplicationResult[Masternode] = {
      if (masternode.ip.startsWith(ipAddress.string)) {
        Future.successful(Good(masternode))
      } else {
        Future.successful(Bad(MasternodeNotFoundError).accumulating)
      }
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(xsnService))
      .build

  "GET /masternodes" should {
    "return the masternodes" in {
      val expected = masternodes.head
      val response = GET("/masternodes?offset=1&limit=10&orderBy=activeSeconds:desc")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "total").as[Int] mustEqual 2

      val jsonList = (json \ "data").as[List[JsValue]]
      jsonList.size mustEqual 1

      val item = jsonList.head
      (item \ "txid").as[String] mustEqual expected.txid.string
      (item \ "payee").as[String] mustEqual expected.payee.string
      (item \ "ip").as[String] mustEqual expected.ip
      (item \ "protocol").as[String] mustEqual expected.protocol
      (item \ "lastSeen").as[Long] mustEqual expected.lastSeen
      (item \ "activeSeconds").as[Long] mustEqual expected.activeSeconds
      (item \ "status").as[String] mustEqual expected.status
    }
  }

  "GET /masternodes/:ip" should {
    "return the masternode" in {
      val expected = masternode
      val response = GET("/masternodes/45.32.148.13")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "activeSeconds").as[Long] mustEqual expected.activeSeconds
      (json \ "ip").as[String] mustEqual expected.ip
      (json \ "lastSeen").as[Long] mustEqual expected.lastSeen
      (json \ "payee").as[String] mustEqual expected.payee.string
      (json \ "protocol").as[String] mustEqual expected.protocol
      (json \ "status").as[String] mustEqual expected.status
      (json \ "txid").as[String] mustEqual expected.txid.string
    }

    "fail on masternode not found" in {
      val response = GET("/masternodes/45.32.149.13")
      status(response) mustEqual NOT_FOUND

      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]
      errorList.size mustEqual 1

      val error = errorList.head
      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "ip"
      (error \ "message").as[String].nonEmpty mustEqual true
    }

    "fail on bad ip format" in {
      val response = GET("/masternodes/45.32.149.1333")
      status(response) mustEqual BAD_REQUEST

      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]
      errorList.size mustEqual 1

      val error = errorList.head
      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "ip"
      (error \ "message").as[String].nonEmpty mustEqual true
    }
  }
}
