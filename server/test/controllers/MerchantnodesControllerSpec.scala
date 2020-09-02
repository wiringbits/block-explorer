package controllers

import com.xsn.explorer.models.rpc.Merchantnode
import com.alexitc.playsonify.play.PublicErrorRenderer
import com.xsn.explorer.models.values.{TransactionId, _}
import com.xsn.explorer.services.synchronizer.repository.MerchantnodeRepository
import controllers.common.MyAPISpec
import play.api.inject.bind
import org.mockito.MockitoSugar._
import org.mockito.ArgumentMatchersSugar._
import play.api.libs.json.JsValue
import org.scalactic.Equality
import play.api.test.Helpers._
import com.xsn.explorer.models.values.IPAddress

import scala.concurrent.Future

class MerchantnodesControllerSpec extends MyAPISpec {

  val merchantnodes = List(
    Merchantnode(
      pubkey = "36383165613065623435373332353634303664656666653535303735616465343966306433363232",
      txid = TransactionId.from("c3efb8b60bda863a3a963d340901dc2b870e6ea51a34276a8f306d47ffb94f01").get,
      ip = "45.77.136.212:62583",
      protocol = "70208",
      status = "WATCHDOG_EXPIRED",
      activeSeconds = 513323,
      lastSeen = 1524349009,
      payee = Address.from("XqdmM7rop8Sdgn8UjyNh3Povc3rhNSXYw2").get
    ),
    Merchantnode(
      pubkey = "36383165613065623435373332353634303664656666653535303735616465343966306433363233",
      txid = TransactionId.from("b02f99d87194c9400ab147c070bf621770684906dedfbbe9ba5f3a35c26b8d01").get,
      ip = "45.32.148.13:62583",
      protocol = "70208",
      status = "ENABLED",
      activeSeconds = 777344,
      lastSeen = 1524349028,
      payee = Address.from("XdNDRAiMUC9KiVRzhCTg9w44jQRdCpCRe3").get
    )
  )

  val merchantnode = merchantnodes.last

  val merchantnodeRepositoryMock = mock[MerchantnodeRepository]

  override val application = guiceApplicationBuilder
    .overrides(bind[MerchantnodeRepository].to(merchantnodeRepositoryMock))
    .build()

  "GET /merchantnodes" should {
    "return the merchantnodes" in {
      val expected = merchantnodes.head
      when(merchantnodeRepositoryMock.getAll()).thenReturn(Future.successful(merchantnodes))

      val response = GET("/merchantnodes?offset=1&limit=10&orderBy=activeSeconds:desc")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "total").as[Int] mustEqual 2

      val jsonList = (json \ "data").as[List[JsValue]]
      jsonList.size mustEqual 1

      val item = jsonList.head
      (item \ "pubkey").as[String] mustEqual expected.pubkey
      (item \ "txid").as[String] mustEqual expected.txid.string
      (item \ "payee").as[String] mustEqual expected.payee.string
      (item \ "ip").as[String] mustEqual expected.ip
      (item \ "protocol").as[String] mustEqual expected.protocol
      (item \ "lastSeen").as[Long] mustEqual expected.lastSeen
      (item \ "activeSeconds").as[Long] mustEqual expected.activeSeconds
      (item \ "status").as[String] mustEqual expected.status
    }
  }

  "GET /merchantnodes/:ip" should {

    implicit val ipEquality: Equality[IPAddress] = new Equality[IPAddress] {
      override def areEqual(a: IPAddress, b: Any): Boolean = {
        b match {
          case ip: IPAddress => a.string == ip.string
          case _ => false
        }
      }
    }

    "return the merchantnode" in {
      val expected = merchantnode
      val ip = new IPAddress("45.32.148.13")

      when(merchantnodeRepositoryMock.find(eqTo(ip)))
        .thenReturn(Future.successful(Some(merchantnode)))

      val response = GET("/merchantnodes/45.32.148.13")
      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "pubkey").as[String] mustEqual expected.pubkey
      (json \ "activeSeconds").as[Long] mustEqual expected.activeSeconds
      (json \ "ip").as[String] mustEqual expected.ip
      (json \ "lastSeen").as[Long] mustEqual expected.lastSeen
      (json \ "payee").as[String] mustEqual expected.payee.string
      (json \ "protocol").as[String] mustEqual expected.protocol
      (json \ "status").as[String] mustEqual expected.status
      (json \ "txid").as[String] mustEqual expected.txid.string
    }

    "fail on merchantnode not found" in {
      val ip = new IPAddress("45.32.149.13")
      when(merchantnodeRepositoryMock.find(eqTo(ip)))
        .thenReturn(Future.successful(None))

      val response = GET("/merchantnodes/45.32.149.13")
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
      val ip = new IPAddress("45.32.149.1333")
      when(merchantnodeRepositoryMock.find(eqTo(ip))).thenReturn(Future.failed(new Exception))

      val response = GET("/merchantnodes/45.32.149.1333")
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
