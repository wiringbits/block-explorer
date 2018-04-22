package controllers

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.helpers.DummyXSNService
import com.xsn.explorer.models.{Address, TransactionId}
import com.xsn.explorer.models.rpc.Masternode
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{Good, One, Or}
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
      lastSeen = 1524297814L,
      payee = Address.from("XqdmM7rop8Sdgn8UjyNh3Povc3rhNSXYw2").get),

    Masternode(
      txid = TransactionId.from("b02f99d87194c9400ab147c070bf621770684906dedfbbe9ba5f3a35c26b8d01").get,
      ip = "45.32.148.13:62583",
      protocol = "70208",
      status = "ENABLED",
      activeSeconds = 777344,
      lastSeen = 1524312645L,
      payee = Address.from("XdNDRAiMUC9KiVRzhCTg9w44jQRdCpCRe3").get)
  )

  val xsnService = new DummyXSNService {
    override def getMasternodes(): FutureApplicationResult[List[Masternode]] = {
      Future.successful(Good(masternodes))
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(xsnService))
      .build

  "GET /masternodes" should {
    "return the masternodes" in {
      val expected = masternodes.head
      val response = GET("/masternodes?offset=1&limit=10&orderByactiveSeconds:desc")
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
}
