package com.xsn.explorer.services.synchronizer

import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.config.ExplorerConfig
import com.xsn.explorer.data.async.BlockFutureDataHandler
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models._
import com.xsn.explorer.models.transformers.toPersistedBlock
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.{BlockService, TransactionCollectorService, XSNService}
import com.xsn.explorer.util.Extensions.FutureOrExt
import javax.inject.Inject
import kamon.Kamon
import org.scalactic.{Bad, Good}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private[synchronizer] class LedgerSynchronizationOps @Inject()(
    explorerConfig: ExplorerConfig,
    blockDataHandler: BlockFutureDataHandler,
    xsnService: XSNService,
    blockService: BlockService,
    transactionCollectorService: TransactionCollectorService
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getLatestLedgerBlock: FutureApplicationResult[Option[persisted.Block]] = {
    blockDataHandler
      .getLatestBlock()
      .toFutureOr
      .map(Option.apply)
      .recoverFrom(BlockNotFoundError)(None)
      .toFuture
  }

  def getRPCBlock(blockhash: Blockhash): FutureApplicationResult[rpc.Block.Canonical] = {
    val result = for {
      rpcBlock <- xsnService.getBlock(blockhash).toFutureOr
    } yield {
      if (explorerConfig.liteVersionConfig.enabled &&
        rpcBlock.height.int < explorerConfig.liteVersionConfig.syncTransactionsFromBlock) {

        // lite version, ignore transactions
        rpcBlock.copy(transactions = List.empty)
      } else {
        rpcBlock
      }
    }

    result.toFuture
  }

  def getFullRPCBlock(blockhash: Blockhash): FutureApplicationResult[rpc.Block.HasTransactions[rpc.TransactionVIN]] = {
    val span = Kamon
      .spanBuilder(operationName = "getFullRPCBlock")
      .tag("blockhash", blockhash.string)
      .start()

    import io.scalaland.chimney.dsl._

    val partial = for {
      // we need to get the canonical block in order to evalu
      rpcBlock <- xsnService.getBlock(blockhash).toFutureOr
    } yield {
      if (explorerConfig.liteVersionConfig.enabled &&
        rpcBlock.height.int < explorerConfig.liteVersionConfig.syncTransactionsFromBlock) {

        // lite version, ignore transactions
        val liteBlock = rpcBlock
          .into[rpc.Block.HasTransactions[rpc.TransactionVIN]]
          .withFieldConst(_.transactions, List.empty)
          .transform
        Future.successful(Good(liteBlock))
      } else {
        xsnService.getFullBlock(blockhash)
      }
    }

    val result = partial.flatMap(_.toFutureOr).toFuture

    result.onComplete {
      case Success(_) => span.finish()
      case Failure(exception) => span.fail(exception)
    }

    result
  }

  def getBlockData(rpcBlock: rpc.Block[_]): FutureApplicationResult[BlockData] = {
    val span = Kamon
      .spanBuilder(operationName = "getBlockData")
      .tag("hash", rpcBlock.hash.string)
      .tag("height", rpcBlock.height.int.toLong)
      .start()

    val result = for {
      extractionMethod <- blockService.extractionMethod(rpcBlock).toFutureOr
      rewards <- getBlockRewards(rpcBlock, extractionMethod).toFutureOr
      data <- transactionCollectorService.collect(rpcBlock).toFutureOr
      (transactions, contracts, filterFactory) = data
      validContracts <- getValidContracts(contracts).toFutureOr
      filteredTransactions = transactions.filter(_.blockhash == rpcBlock.hash)
    } yield {
      if (transactions.size != filteredTransactions.size) {
        // see https://github.com/bitpay/insight-api/issues/42
        logger.warn(
          s"The block = ${rpcBlock.hash} has phantom ${transactions.size - filteredTransactions.size} transactions, they are being discarded"
        )
      }
      val block = toPersistedBlock(rpcBlock, extractionMethod).withTransactions(filteredTransactions)
      (block, validContracts, filterFactory, rewards)
    }

    result.toFuture.onComplete {
      case Success(_) => span.finish()
      case Failure(exception) => span.fail(exception)
    }

    result.toFuture
  }

  private def getBlockRewards(
      rpcBlock: rpc.Block[_],
      extractionMethod: BlockExtractionMethod
  ): FutureApplicationResult[Option[BlockRewards]] = {
    blockService.getBlockRewards(rpcBlock, extractionMethod).map {
      case Good(reward) => Good(Some(reward))
      case Bad(_) => Good(None)
    }
  }

  private def getValidContracts(contracts: List[TPoSContract]): FutureApplicationResult[List[TPoSContract]] = {
    val listF = contracts
      .map { contract =>
        xsnService
          .isTPoSContract(contract.txid)
          .toFutureOr
          .map { valid =>
            if (valid) Some(contract)
            else None
          }
          .toFuture
      }

    val futureList = Future.sequence(listF)
    futureList.map { list =>
      val x = list.flatMap {
        case Good(a) => a.map(Good(_))
        case Bad(e) => Some(Bad(e))
      }

      val initial: ApplicationResult[List[TPoSContract]] = Good(List.empty)
      x.foldLeft(initial) {
        case (acc, cur) =>
          cur match {
            case Good(contract) => acc.map(contract :: _)
            case Bad(e) => acc.badMap(prev => prev ++ e)
          }
      }
    }
  }
}
