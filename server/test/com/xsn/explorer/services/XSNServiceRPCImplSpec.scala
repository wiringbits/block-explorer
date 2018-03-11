package com.xsn.explorer.services

import com.xsn.explorer.config.RPCConfig
import com.xsn.explorer.errors.{TransactionNotFoundError, XSNMessageError, XSNUnexpectedResponseError}
import com.xsn.explorer.helpers.Executors
import com.xsn.explorer.models.TransactionId
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalactic.Bad
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.Future

class XSNServiceRPCImplSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar {

  val ws = mock[WSClient]
  val ec = Executors.externalServiceEC
  val config = new RPCConfig {
    override def password: RPCConfig.Password = RPCConfig.Password("pass")
    override def host: RPCConfig.Host = RPCConfig.Host("localhost")
    override def username: RPCConfig.Username = RPCConfig.Username("user")
  }

  val request = mock[WSRequest]
  val response = mock[WSResponse]
  when(ws.url(anyString)).thenReturn(request)
  when(request.withAuth(anyString(), anyString(), any())).thenReturn(request)
  when(request.withHttpHeaders(any())).thenReturn(request)

  val service = new XSNServiceRPCImpl(ws, config)(ec)
  val txid = TransactionId.from("024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c").get

  "getTransaction" should {
    "handle a successful result" in {
      val responseBody =
        """
          |{
          |    "result": {
          |        "blockhash": "000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7",
          |        "blocktime": 1520276270,
          |        "confirmations": 5347,
          |        "height": 1,
          |        "hex": "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff03510101ffffffff010000000000000000232103e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029ac00000000",
          |        "locktime": 0,
          |        "size": 98,
          |        "time": 1520276270,
          |        "txid": "024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c",
          |        "version": 1,
          |        "vin": [
          |            {
          |                "coinbase": "510101",
          |                "sequence": 4294967295
          |            }
          |        ],
          |        "vout": [
          |            {
          |                "n": 0,
          |                "scriptPubKey": {
          |                    "addresses": [
          |                        "XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9"
          |                    ],
          |                    "asm": "03e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029 OP_CHECKSIG",
          |                    "hex": "2103e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029ac",
          |                    "reqSigs": 1,
          |                    "type": "pubkey"
          |                },
          |                "value": 0,
          |                "valueSat": 0
          |            }
          |        ]
          |    },
          |    "error": null,
          |    "id": null
          |}
        """.stripMargin.trim

      val json = Json.parse(responseBody)

      when(response.status).thenReturn(200)
      when(response.json).thenReturn(json)
      when(request.post[String](anyString())(any())).thenReturn(Future.successful(response))

      whenReady(service.getTransaction(txid)) { result =>
        result.isGood mustEqual true
      }
    }

    "handle transaction not found" in {
      val responseBody = """{"result":null,"error":{"code":-5,"message":"No information available about transaction"},"id":null}"""
      val json = Json.parse(responseBody)
      when(response.status).thenReturn(200)
      when(response.json).thenReturn(json)
      when(request.post[String](anyString())(any())).thenReturn(Future.successful(response))

      whenReady(service.getTransaction(txid)) { result =>
        result mustEqual Bad(TransactionNotFoundError).accumulating
      }
    }

    "handle error with message" in {
      val responseBody = """{"result":null,"error":{"code":-32600,"message":"Params must be an array"},"id":null}"""
      val json = Json.parse(responseBody)

      when(response.status).thenReturn(200)
      when(response.json).thenReturn(json)
      when(request.post[String](anyString())(any())).thenReturn(Future.successful(response))

      whenReady(service.getTransaction(txid)) { result =>
        val error = XSNMessageError("Params must be an array")
        result mustEqual Bad(error).accumulating
      }
    }

    "handle unexpected error" in {
      val responseBody = """{"result":null,"error":{},"id":null}"""
      val json = Json.parse(responseBody)

      when(response.status).thenReturn(200)
      when(response.json).thenReturn(json)
      when(request.post[String](anyString())(any())).thenReturn(Future.successful(response))

      whenReady(service.getTransaction(txid)) { result =>
        result mustEqual Bad(XSNUnexpectedResponseError).accumulating
      }
    }
  }
}
