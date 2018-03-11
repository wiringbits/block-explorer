package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.models.ApplicationError
import com.xsn.explorer.config.RPCConfig
import com.xsn.explorer.errors.{TransactionNotFoundError, XSNMessageError, XSNUnexpectedResponseError}
import com.xsn.explorer.executors.ExternalServiceExecutionContext
import com.xsn.explorer.models.TransactionId
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNull, JsValue}
import play.api.libs.ws.{WSAuthScheme, WSClient}

import scala.util.Try

trait XSNService {

  def getTransaction(txid: TransactionId): FutureApplicationResult[JsValue]
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

  // TODO: cache successful results
  override def getTransaction(txid: TransactionId): FutureApplicationResult[JsValue] = {
    server
        .post(s"""{ "jsonrpc": "1.0", "method": "getrawtransaction", "params": ["${txid.string}", 1] }""")
        .map { response =>

          val maybe = Try(response.json)
              .toOption
              .flatMap { json =>
                (json \ "result")
                    .asOpt[JsValue]
                    .filter(_ != JsNull)
                    .map { Good(_) }
                    .orElse {
                      mapError(json)
                          .map(Bad.apply)
                          .map(_.accumulating)
                    }
              }

          maybe.getOrElse {
            logger.warn(s"Unexpected response from XSN Server, txid = ${txid.string}, response = ${response.body}")

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
  }

  private def mapError(json: JsValue): Option[ApplicationError] = {
    val jsonErrorMaybe = (json \ "error")
        .asOpt[JsValue]
        .filter(_ != JsNull)

    jsonErrorMaybe
        .flatMap { jsonError =>
          // from error code if possible
          (jsonError \ "code")
              .asOpt[Int]
              .flatMap(fromErrorCode)
              .orElse {
                // from message
                (jsonError \ "message")
                    .asOpt[String]
                    .filter(_.nonEmpty)
                    .map(XSNMessageError.apply)
              }
        }
  }

  private def fromErrorCode(code: Int): Option[ApplicationError] = code match {
    case -5 => Some(TransactionNotFoundError)
    case _ => None
  }
}
