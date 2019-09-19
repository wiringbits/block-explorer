package com.xsn.explorer.services

import java.net.ConnectException

import akka.actor.Scheduler
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.alexitc.playsonify.models.ApplicationError
import com.xsn.explorer.config.{ExplorerConfig, RPCConfig, RetryConfig}
import com.xsn.explorer.errors._
import com.xsn.explorer.executors.ExternalServiceExecutionContext
import com.xsn.explorer.models._
import com.xsn.explorer.models.values._
import com.xsn.explorer.util.RetryableFuture
import javax.inject.Inject
import org.scalactic.{Bad, Good, One}
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait XSNService {

  def getTransaction(txid: TransactionId): FutureApplicationResult[rpc.Transaction[rpc.TransactionVIN]]

  def getRawTransaction(txid: TransactionId): FutureApplicationResult[JsValue]

  def getBlock(blockhash: Blockhash): FutureApplicationResult[rpc.Block.Canonical]

  def getFullBlock(blockhash: Blockhash): FutureApplicationResult[rpc.Block.HasTransactions[rpc.TransactionVIN]]

  def getRawBlock(blockhash: Blockhash): FutureApplicationResult[JsValue]

  def getBlockhash(height: Height): FutureApplicationResult[Blockhash]

  def getLatestBlock(): FutureApplicationResult[rpc.Block.Canonical]

  def getServerStatistics(): FutureApplicationResult[rpc.ServerStatistics]

  def getMasternodeCount(): FutureApplicationResult[Int]

  def getDifficulty(): FutureApplicationResult[BigDecimal]

  def getMasternodes(): FutureApplicationResult[List[rpc.Masternode]]

  def getMasternode(ipAddress: IPAddress): FutureApplicationResult[rpc.Masternode]

  def getUnspentOutputs(address: Address): FutureApplicationResult[JsValue]

  def sendRawTransaction(hex: HexString): FutureApplicationResult[String]

  def isTPoSContract(txid: TransactionId): FutureApplicationResult[Boolean]

  def estimateSmartFee(confirmationsTarget: Int): FutureApplicationResult[JsValue]

  def getTxOut(txid: TransactionId, index: Int, includeMempool: Boolean): FutureApplicationResult[JsValue]

  def cleanGenesisBlock(block: rpc.Block.Canonical): rpc.Block.Canonical = {
    Option(block)
      .filter(_.hash == genesisBlockhash)
      .map(_.copy(transactions = List.empty))
      .getOrElse(block)
  }

  def genesisBlockhash: Blockhash

}

class XSNServiceRPCImpl @Inject()(
    ws: WSClient,
    rpcConfig: RPCConfig,
    explorerConfig: ExplorerConfig,
    retryConfig: RetryConfig
)(
    implicit ec: ExternalServiceExecutionContext,
    scheduler: Scheduler
) extends XSNService {

  private val defaultErrorCodeMapper = Map(-28 -> XSNWarmingUp)
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val server = ws
    .url(rpcConfig.host.string)
    .withAuth(rpcConfig.username.string, rpcConfig.password.string, WSAuthScheme.BASIC)
    .withHttpHeaders("Content-Type" -> "text/plain")

  private def retrying[A](f: => FutureApplicationResult[A]): FutureApplicationResult[A] = {
    val retry = RetryableFuture.withExponentialBackoff[ApplicationResult[A]](
      retryConfig.initialDelay,
      retryConfig.maxDelay
    )
    val shouldRetry: Try[ApplicationResult[A]] => Boolean = {
      case Success(Bad(One(XSNWorkQueueDepthExceeded))) => true
      case Success(Bad(One(XSNWarmingUp))) => true
      case Failure(_: ConnectException) => true
      case _ => false
    }

    retry(shouldRetry) {
      f
    }
  }

  override def getTransaction(txid: TransactionId): FutureApplicationResult[rpc.Transaction[rpc.TransactionVIN]] = {
    val errorCodeMapper = Map(-5 -> TransactionError.NotFound(txid))

    val result = retrying {
      server
        .post(s"""{ "jsonrpc": "1.0", "method": "getrawtransaction", "params": ["${txid.string}", 1] }""")
        .map { response =>
          val maybe = getResult[rpc.Transaction[rpc.TransactionVIN]](response, errorCodeMapper)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, txid = ${txid.string}, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get transaction $txid, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getRawTransaction(txid: TransactionId): FutureApplicationResult[JsValue] = {
    val errorCodeMapper = Map(-5 -> TransactionError.NotFound(txid))

    val result = retrying {
      server
        .post(s"""{ "jsonrpc": "1.0", "method": "getrawtransaction", "params": ["${txid.string}", 1] }""")
        .map { response =>
          val maybe = getResult[JsValue](response, errorCodeMapper)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, txid = ${txid.string}, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get raw transaction $txid, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getBlock(blockhash: Blockhash): FutureApplicationResult[rpc.Block.Canonical] = {
    val errorCodeMapper = Map(-5 -> BlockNotFoundError)
    val body = s"""{ "jsonrpc": "1.0", "method": "getblock", "params": ["${blockhash.string}"] }"""

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[rpc.Block.Canonical](response, errorCodeMapper)
          maybe
            .map {
              case Good(block) => Good(cleanGenesisBlock(block))
              case x => x
            }
            .getOrElse {
              logger.debug(
                s"Unexpected response from XSN Server, blockhash = ${blockhash.string}, status = ${response.status}, response = ${response.body}"
              )

              Bad(XSNUnexpectedResponseError).accumulating
            }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get block $blockhash, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getFullBlock(
      blockhash: Blockhash
  ): FutureApplicationResult[rpc.Block.HasTransactions[rpc.TransactionVIN]] = {
    val errorCodeMapper = Map(-5 -> BlockNotFoundError)
    val body = s"""{ "jsonrpc": "1.0", "method": "getblock", "params": ["${blockhash.string}", 2] }"""

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[rpc.Block.HasTransactions[rpc.TransactionVIN]](response, errorCodeMapper)
          maybe
            .map {
              case Good(block) => Good(block)
              case x => x
            }
            .getOrElse {
              logger.debug(
                s"Unexpected response from XSN Server, blockhash = ${blockhash.string}, status = ${response.status}, response = ${response.body}"
              )

              Bad(XSNUnexpectedResponseError).accumulating
            }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get full block $blockhash, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getRawBlock(blockhash: Blockhash): FutureApplicationResult[JsValue] = {
    val errorCodeMapper = Map(-5 -> BlockNotFoundError)
    val body = s"""{ "jsonrpc": "1.0", "method": "getblock", "params": ["${blockhash.string}"] }"""

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[JsValue](response, errorCodeMapper)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, blockhash = ${blockhash.string}, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get raw block $blockhash, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getBlockhash(height: Height): FutureApplicationResult[Blockhash] = {
    val errorCodeMapper = Map(-8 -> BlockNotFoundError)
    val body = s"""{ "jsonrpc": "1.0", "method": "getblockhash", "params": [${height.int}] }"""

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[Blockhash](response, errorCodeMapper)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, blockhash = ${height.int}, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get blockhash $height, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getLatestBlock(): FutureApplicationResult[rpc.Block.Canonical] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "getbestblockhash",
                  |  "params": []
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .flatMap { response =>
          val result = for {
            blockhash <- getResult[Blockhash](response)
              .orElse {
                logger.debug(
                  s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
                )
                None
              }
              .getOrElse(Bad(XSNUnexpectedResponseError).accumulating)
              .toFutureOr

            block <- getBlock(blockhash).map {
              case Good(block) => Good(cleanGenesisBlock(block))
              case x => x
            }.toFutureOr
          } yield block

          result.toFuture
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get latest block, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getServerStatistics(): FutureApplicationResult[rpc.ServerStatistics] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "gettxoutsetinfo",
                  |  "params": []
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[rpc.ServerStatistics](response)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get server statistics, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getMasternodeCount(): FutureApplicationResult[Int] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "masternode",
                  |  "params": ["count"]
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[Int](response)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get master node count, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getDifficulty(): FutureApplicationResult[BigDecimal] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "getdifficulty",
                  |  "params": []
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[BigDecimal](response)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get difficulty, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getMasternodes(): FutureApplicationResult[List[rpc.Masternode]] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "masternode",
                  |  "params": ["list", "full"]
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[Map[String, String]](response)
            .map {
              case Good(map) => Good(rpc.Masternode.fromMap(map))
              case Bad(errors) => Bad(errors)
            }

          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get master nodes, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getMasternode(ipAddress: IPAddress): FutureApplicationResult[rpc.Masternode] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "masternode",
                  |  "params": ["list", "full", "${ipAddress.string}"]
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[Map[String, String]](response)
            .map {
              case Good(map) =>
                rpc.Masternode
                  .fromMap(map)
                  .headOption
                  .map(Good(_))
                  .getOrElse(Bad(MasternodeNotFoundError).accumulating)

              case Bad(errors) => Bad(errors)
            }

          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get master node $ipAddress, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getUnspentOutputs(address: Address): FutureApplicationResult[JsValue] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "getaddressutxos",
                  |  "params": [
                  |    { "addresses": ["${address.string}"] }
                  |  ]
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[JsValue](response)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get unspent outputs $address, errors = $errors")
      case _ => ()
    }

    result
  }

  override def sendRawTransaction(hex: HexString): FutureApplicationResult[String] = {
    val errorCodeMapper = Map(
      -26 -> TransactionError.InvalidRawTransaction,
      -22 -> TransactionError.InvalidRawTransaction,
      -27 -> TransactionError.RawTransactionAlreadyExists
    )

    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "sendrawtransaction",
                  |  "params": ["${hex.string}"]
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[String](response, errorCodeMapper).map {
            _.map(_.toString())
          }

          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to send raw transaction $hex, errors = $errors")
      case _ => ()
    }

    result
  }

  override def isTPoSContract(txid: TransactionId): FutureApplicationResult[Boolean] = {
    val innerBody = Json.obj("txid" -> txid.string, "check_spent" -> 0)
    val body = Json.obj(
      "jsonrpc" -> "1.0",
      "method" -> "tposcontract",
      "params" -> List(
        JsString("validate"),
        JsString(innerBody.toString())
      )
    )

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[String](response)
            .map {
              _.map(_ == "Contract is valid")
            }

          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get is TPoS contract $txid, errors = $errors")
      case _ => ()
    }

    result
  }

  override def estimateSmartFee(confirmationsTarget: Int): FutureApplicationResult[JsValue] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "estimatesmartfee",
                  |  "params": [$confirmationsTarget]
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[JsValue](response)

          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to estimate smart fee $confirmationsTarget, errors = $errors")
      case _ => ()
    }

    result
  }

  override def getTxOut(txid: TransactionId, index: Int, includeMempool: Boolean): FutureApplicationResult[JsValue] = {
    val body = s"""
                  |{
                  |  "jsonrpc": "1.0",
                  |  "method": "gettxout",
                  |  "params": ["${txid.string}", $index, $includeMempool]
                  |}
                  |""".stripMargin

    val result = retrying {
      server
        .post(body)
        .map { response =>
          val maybe = getResult[JsValue](response)
          maybe.getOrElse {
            logger.debug(
              s"Unexpected response from XSN Server, status = ${response.status}, response = ${response.body}"
            )

            Bad(XSNUnexpectedResponseError).accumulating
          }
        }
    }

    result.foreach {
      case Bad(errors) => logger.warn(s"Failed to get TxOut, errors = $errors")
      case _ => ()
    }

    result
  }

  private def mapError(json: JsValue, errorCodeMapper: Map[Int, ApplicationError]): Option[ApplicationError] = {
    val jsonErrorMaybe = (json \ "error")
      .asOpt[JsValue]
      .filter(_ != JsNull)

    val errorMaybe = jsonErrorMaybe
      .flatMap { jsonError =>
        // from error code if possible
        (jsonError \ "code")
          .asOpt[Int]
          .flatMap(code => errorCodeMapper.get(code) orElse defaultErrorCodeMapper.get(code))
          .orElse {
            // from message
            (jsonError \ "message")
              .asOpt[String]
              .filter(_.nonEmpty)
              .map(XSNMessageError.apply)
          }
      }

    errorMaybe
      .collect {
        case XSNMessageError("Work queue depth exceeded") => XSNWorkQueueDepthExceeded
      }
      .orElse(errorMaybe)
  }

  private def getResult[A](response: WSResponse, errorCodeMapper: Map[Int, ApplicationError] = Map.empty)(
      implicit reads: Reads[A]
  ): Option[ApplicationResult[A]] = {

    val maybe = Option(response)
      .filter(_.status == 200)
      .flatMap { r =>
        Try(r.json).toOption
      }
      .flatMap { json =>
        if (logger.isDebugEnabled) {
          val x = (json \ "result").validate[A]
          x.asEither.left.foreach { errors =>
            val msg = errors
              .map { case (path, error) => path.toJsonString -> error.toString() }
              .mkString(", ")
            logger.debug(s"Failed to decode result, errors = ${msg}")
          }
        }
        (json \ "result")
          .asOpt[A]
          .map { Good(_) }
          .orElse {
            mapError(json, errorCodeMapper)
              .map(Bad.apply)
              .map(_.accumulating)
          }
      }

    maybe
      .orElse {
        // if there is no result nor error, it is probably that the server returned non 200 status
        Try(response.json).toOption
          .flatMap { json =>
            mapError(json, errorCodeMapper)
          }
          .map { e =>
            Bad(e).accumulating
          }
      }
      .orElse {
        // if still there is no error, the response might not be a json
        Try(response.body).collect {
          case "Work queue depth exceeded" => Bad(XSNWorkQueueDepthExceeded).accumulating
        }.toOption
      }
  }

  override val genesisBlockhash: Blockhash = explorerConfig.genesisBlock

}
