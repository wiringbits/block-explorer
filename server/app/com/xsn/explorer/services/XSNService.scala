package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.alexitc.playsonify.models.ApplicationError
import com.xsn.explorer.config.RPCConfig
import com.xsn.explorer.errors._
import com.xsn.explorer.executors.ExternalServiceExecutionContext
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.{AddressBalance, Block, Transaction}
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNull, JsValue, Reads}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}

import scala.util.Try

trait XSNService {

  def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction]

  def getAddressBalance(address: Address): FutureApplicationResult[AddressBalance]

  def getTransactions(address: Address): FutureApplicationResult[List[TransactionId]]

  def getBlock(blockhash: Blockhash): FutureApplicationResult[Block]

  def getLatestBlock(): FutureApplicationResult[Block]
}

class XSNServiceRPCImpl @Inject() (
    ws: WSClient,
    rpcConfig: RPCConfig)(
    implicit ec: ExternalServiceExecutionContext)
    extends XSNService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val server = ws.url(rpcConfig.host.string)
      .withAuth(rpcConfig.username.string, rpcConfig.password.string, WSAuthScheme.BASIC)
      .withHttpHeaders("Content-Type" -> "text/plain")


  override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction] = {
    val errorCodeMapper = Map(-5 -> TransactionNotFoundError)

    server
        .post(s"""{ "jsonrpc": "1.0", "method": "getrawtransaction", "params": ["${txid.string}", 1] }""")
        .map { response =>

          val maybe = getResult[Transaction](response, errorCodeMapper)
          maybe.getOrElse {
            logger.warn(s"Unexpected response from XSN Server, txid = ${txid.string}, status = ${response.status}, response = ${response.body}")

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
  }

  override def getAddressBalance(address: Address): FutureApplicationResult[AddressBalance] = {
    val body = s"""
         |{
         |  "jsonrpc": "1.0",
         |  "method": "getaddressbalance",
         |  "params": [
         |    { "addresses": ["${address.string}"] }
         |  ]
         |}
         |""".stripMargin

    // the network returns 0 for valid addresses
    val errorCodeMapper = Map(-5 -> AddressFormatError)

    server
        .post(body)
        .map { response =>

          val maybe = getResult[AddressBalance](response, errorCodeMapper)
          maybe.getOrElse {
            logger.warn(s"Unexpected response from XSN Server, status = ${response.status}, address = ${address.string}, response = ${response.body}")

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
  }

  override def getTransactions(address: Address): FutureApplicationResult[List[TransactionId]] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "getaddresstxids",
                  |  "params": [
                  |    { "addresses": ["${address.string}"] }
                  |  ]
                  |}
                  |""".stripMargin

    // the network returns 0 for valid addresses
    val errorCodeMapper = Map(-5 -> AddressFormatError)

    server
        .post(body)
        .map { response =>

          val maybe = getResult[List[TransactionId]](response, errorCodeMapper)
          maybe.getOrElse {
            logger.warn(s"Unexpected response from XSN Server, status = ${response.status}, address = ${address.string}, response = ${response.body}")

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
  }

  override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block] = {
    val errorCodeMapper = Map(-5 -> BlockNotFoundError)
    val body = s"""{ "jsonrpc": "1.0", "method": "getblock", "params": ["${blockhash.string}"] }"""

    server
        .post(body)
        .map { response =>

          val maybe = getResult[Block](response, errorCodeMapper)
          maybe.getOrElse {
            logger.warn(s"Unexpected response from XSN Server, txid = ${blockhash.string}, status = ${response.status}, response = ${response.body}")

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
  }

  override def getLatestBlock(): FutureApplicationResult[Block] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "getbestblockhash",
                  |  "params": []
                  |}
                  |""".stripMargin

    server
        .post(body)
        .flatMap { response =>

          val result = for {
            blockhash <- getResult[Blockhash](response)
                .orElse {
                  logger.warn(s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}")
                  None
                }
                .getOrElse(Bad(XSNUnexpectedResponseError).accumulating)
                .toFutureOr

            block <- getBlock(blockhash).toFutureOr
          } yield block

          result.toFuture
        }
  }

  private def mapError(json: JsValue, errorCodeMapper: Map[Int, ApplicationError]): Option[ApplicationError] = {
    val jsonErrorMaybe = (json \ "error")
        .asOpt[JsValue]
        .filter(_ != JsNull)

    jsonErrorMaybe
        .flatMap { jsonError =>
          // from error code if possible
          (jsonError \ "code")
              .asOpt[Int]
              .flatMap(errorCodeMapper.get)
              .orElse {
                // from message
                (jsonError \ "message")
                    .asOpt[String]
                    .filter(_.nonEmpty)
                    .map(XSNMessageError.apply)
              }
        }
  }

  private def getResult[A](
      response: WSResponse,
      errorCodeMapper: Map[Int, ApplicationError] = Map.empty)(
      implicit reads: Reads[A]): Option[ApplicationResult[A]] = {

    val maybe = Option(response)
        .filter(_.status == 200)
        .flatMap { r => Try(r.json).toOption }
        .flatMap { json =>
          (json \ "result")
              .asOpt[A]
              .map { Good(_) }
              .orElse {
                mapError(json, errorCodeMapper)
                    .map(Bad.apply)
                    .map(_.accumulating)
              }
        }

    // if there is no result nor error, it is probably that the server returned non 200 status
    maybe.orElse {
      Try(response.json)
          .toOption
          .flatMap { json => mapError(json, errorCodeMapper) }
          .map { e => Bad(e).accumulating }
    }
  }
}
