package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.BlockRewardsNotFoundError
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.{Block, TransactionVIN}
import com.xsn.explorer.models.values.{Blockhash, Height}
import com.xsn.explorer.services.logic.{BlockLogic, TransactionLogic}
import com.xsn.explorer.util.Extensions.FutureOrExt
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

class BlockService @Inject() (
    xsnService: XSNService,
    blockLogic: BlockLogic,
    transactionLogic: TransactionLogic)(
    implicit ec: ExecutionContext) {

  def getRawBlock(blockhashString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      blockhash <- blockLogic
          .getBlockhash(blockhashString)
          .toFutureOr

      block <- xsnService.getRawBlock(blockhash).toFutureOr
    } yield block

    result.toFuture
  }

  def getRawBlock(height: Height): FutureApplicationResult[JsValue] = {
    val result = for {
      blockhash <- xsnService.getBlockhash(height).toFutureOr
      block <- xsnService.getRawBlock(blockhash).toFutureOr
    } yield block

    result.toFuture
  }

  def getDetails(blockhashString: String): FutureApplicationResult[BlockDetails] = {
    val result = for {
      blockhash <- blockLogic
          .getBlockhash(blockhashString)
          .toFutureOr

      details <- getDetailsPrivate(blockhash).toFutureOr
    } yield details

    result.toFuture
  }

  def getDetails(height: Height): FutureApplicationResult[BlockDetails] = {
    val result = for {
      blockhash <- xsnService
          .getBlockhash(height)
          .toFutureOr

      details <- getDetailsPrivate(blockhash).toFutureOr
    } yield details

    result.toFuture
  }

  private def getDetailsPrivate(blockhash: Blockhash): FutureApplicationResult[BlockDetails] = {
    val result = for {
      block <- xsnService
          .getBlock(blockhash)
          .toFutureOr

      rewards <- getBlockRewards(block)
          .toFutureOr
          .map(Option.apply)
          .recoverFrom(BlockRewardsNotFoundError)(Option.empty)
    } yield BlockDetails(block, rewards)

    result.toFuture
  }

  def getLatestBlocks(): FutureApplicationResult[List[Block]] = {
    /**
     * Temporal workaround to retrieve the latest blocks, they
     * will be retrieved from the database once available.
     */
    val result = for {
      a <- xsnService.getLatestBlock().toFutureOr
      b <- xsnService.getBlock(a.previousBlockhash.get).toFutureOr
      c <- xsnService.getBlock(b.previousBlockhash.get).toFutureOr
      d <- xsnService.getBlock(c.previousBlockhash.get).toFutureOr
      e <- xsnService.getBlock(d.previousBlockhash.get).toFutureOr
      f <- xsnService.getBlock(e.previousBlockhash.get).toFutureOr
      g <- xsnService.getBlock(f.previousBlockhash.get).toFutureOr
      h <- xsnService.getBlock(g.previousBlockhash.get).toFutureOr
      i <- xsnService.getBlock(h.previousBlockhash.get).toFutureOr
      j <- xsnService.getBlock(i.previousBlockhash.get).toFutureOr
    } yield List(a, b, c, d, e, f, g, h, i, j)

    result.toFuture
  }

  def extractionMethod(block: rpc.Block): FutureApplicationResult[BlockExtractionMethod] = {
    if (block.tposContract.isDefined) {
      Future.successful(Good(BlockExtractionMethod.TrustlessProofOfStake))
    } else if (block.transactions.isEmpty) {
      Future.successful(Good(BlockExtractionMethod.ProofOfWork))
    } else {
      isPoS(block)
          .toFutureOr
          .map {
            case true => BlockExtractionMethod.ProofOfStake
            case false => BlockExtractionMethod.ProofOfWork
          }
          .toFuture
    }
  }

  private def isPoS(block: rpc.Block): FutureApplicationResult[Boolean] = {
    val result = for {
      coinbaseTxid <- blockLogic.getCoinbase(block).toFutureOr
      coinbase <- xsnService.getTransaction(coinbaseTxid).toFutureOr
    } yield blockLogic.isPoS(block, coinbase)

    result.toFuture
  }

  private def getBlockRewards(block: Block): FutureApplicationResult[BlockRewards] = {
    if (block.transactions.isEmpty) {
      Future.successful(Bad(BlockRewardsNotFoundError).accumulating)
    } else if (block.isPoW) {
      getPoWBlockRewards(block)
    } else if (block.isPoS) {
      getPoSBlockRewards(block)
    } else {
      getTPoSBlockRewards(block)
    }
  }

  private def getPoWBlockRewards(block: Block): FutureApplicationResult[PoWBlockRewards] = {
    val result = for {
      txid <- blockLogic.getPoWTransactionId(block).toFutureOr
      // TODO: handle tx not found
      tx <- xsnService.getTransaction(txid).toFutureOr
      vout <- transactionLogic.getVOUT(0, tx, BlockRewardsNotFoundError).toFutureOr
      address <- transactionLogic.getAddress(vout, BlockRewardsNotFoundError).toFutureOr
    } yield PoWBlockRewards(BlockReward(address, vout.value))

    result.toFuture
  }

  private def getPoSBlockRewards(block: Block): FutureApplicationResult[PoSBlockRewards] = {
    val result = for {
      coinstakeTxId <- blockLogic
          .getCoinstakeTransactionId(block)
          .toFutureOr
      coinstakeTx <- xsnService
          .getTransaction(coinstakeTxId)
          .toFutureOr
      coinstakeTxVIN <- transactionLogic
          .getVIN(coinstakeTx, BlockRewardsNotFoundError)
          .toFutureOr

      previousToCoinstakeTx <- xsnService
          .getTransaction(coinstakeTxVIN.txid)
          .toFutureOr
      previousToCoinstakeVOUT <- transactionLogic
          .getVOUT(coinstakeTxVIN, previousToCoinstakeTx, BlockRewardsNotFoundError)
          .toFutureOr

      coinstakeAddress <- transactionLogic
          .getAddress(previousToCoinstakeVOUT, BlockRewardsNotFoundError)
          .toFutureOr

      rewards <- blockLogic
          .getPoSRewards(coinstakeTx, coinstakeAddress, previousToCoinstakeVOUT.value)
          .toFutureOr
    } yield rewards

    result.toFuture
  }

  private def getTPoSBlockRewards(block: Block): FutureApplicationResult[BlockRewards] = {
    val result = for {
      coinstakeTxId <- blockLogic
          .getCoinstakeTransactionId(block)
          .toFutureOr
      coinstakeTx <- xsnService
          .getTransaction(coinstakeTxId)
          .toFutureOr
      coinstakeTxVIN <- transactionLogic
          .getVIN(coinstakeTx, BlockRewardsNotFoundError)
          .toFutureOr

      coinstakeInput <- getCoinstakeInput(coinstakeTxVIN).toFutureOr

      tposTxId <- blockLogic
          .getTPoSTransactionId(block)
          .toFutureOr
      tposTx <- xsnService
          .getTransaction(tposTxId)
          .toFutureOr

      addresses <- blockLogic
          .getTPoSAddresses(tposTx)
          .toFutureOr

      (ownerAddress, merchantAddress) = addresses

      rewards <- blockLogic
          .getTPoSRewards(coinstakeTx, ownerAddress, merchantAddress, coinstakeInput)
          .toFutureOr
    } yield rewards

    result.toFuture
  }

  private def getCoinstakeInput(coinstakeTxVIN: TransactionVIN): FutureApplicationResult[BigDecimal] = {
    def loadFromTx = {
      val result = for {
        previousToCoinstakeTx <- xsnService
            .getTransaction(coinstakeTxVIN.txid)
            .toFutureOr
        previousToCoinstakeVOUT <- transactionLogic
            .getVOUT(coinstakeTxVIN, previousToCoinstakeTx, BlockRewardsNotFoundError)
            .toFutureOr
      } yield previousToCoinstakeVOUT.value

      result.toFuture
    }

    coinstakeTxVIN
        .value
        .map(Good(_))
        .map(Future.successful)
        .getOrElse(loadFromTx)
  }
}
