package controllers

import com.alexitc.playsonify.PublicErrorRenderer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.errors.TransactionNotFoundError
import com.xsn.explorer.models.TransactionId
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{Bad, Good}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._

import scala.concurrent.Future

class TransactionsControllerSpec extends MyAPISpec {

  val customXSNService = new XSNService {
    val map = Map(
      TransactionId.from("024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c").get ->
        s"""
           |{
           |    "blockhash": "000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7",
           |    "blocktime": 1520276270,
           |    "confirmations": 5347,
           |    "height": 1,
           |    "hex": "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff03510101ffffffff010000000000000000232103e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029ac00000000",
           |    "locktime": 0,
           |    "size": 98,
           |    "time": 1520276270,
           |    "txid": "024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c",
           |    "version": 1,
           |    "vin": [
           |        {
           |            "coinbase": "510101",
           |            "sequence": 4294967295
           |        }
           |    ],
           |    "vout": [
           |        {
           |            "n": 0,
           |            "scriptPubKey": {
           |                "addresses": [
           |                    "XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9"
           |                ],
           |                "asm": "03e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029 OP_CHECKSIG",
           |                "hex": "2103e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029ac",
           |                "reqSigs": 1,
           |                "type": "pubkey"
           |            },
           |            "value": 0,
           |            "valueSat": 0
           |        }
           |    ]
           |}
         """.stripMargin.trim
    )

    override def getTransaction(txid: TransactionId): FutureApplicationResult[JsValue] = {
      val result = map.get(txid)
          .map(s => Json.toJson(s))
          .map(Good(_))
          .getOrElse {
            Bad(TransactionNotFoundError).accumulating
          }

      Future.successful(result)
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(customXSNService))
      .build()

  "GET /transactions/:txid" should {
    def url(txid: String) = s"/transactions/$txid"

    "return an existing transaction" in {
      val txid = "024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c"
      val response = GET(url(txid))

      status(response) mustEqual OK
      // TODO: Match result
      //val json = contentAsJson(response)
      //(json \ "txid").as[String] mustEqual txid
    }

    "fail on wrong transaction format" in {
      // 63 characters
      val txid = "000001d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c"
      val response = GET(url(txid))

      status(response) mustEqual BAD_REQUEST
      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "transactionId"
      (error \ "message").as[String].nonEmpty mustEqual true
    }

    "fail on unknown transaction" in {
      val txid = "0000001d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c"
      val response = GET(url(txid))

      status(response) mustEqual BAD_REQUEST
      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "transactionId"
      (error \ "message").as[String].nonEmpty mustEqual true
    }
  }
}
