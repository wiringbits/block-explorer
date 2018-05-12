package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.{Block, TransactionVIN}
import com.xsn.explorer.services.logic.{BlockLogic, TransactionLogic}
import org.scalactic.Good

import scala.concurrent.{ExecutionContext, Future}

class BlockService @Inject() (
    xsnService: XSNService,
    blockLogic: BlockLogic,
    transactionLogic: TransactionLogic)(
    implicit ec: ExecutionContext) {

  def getDetails(blockhashString: String): FutureApplicationResult[BlockDetails] = {
    val result = for {
      blockhash <- blockLogic
          .getBlockhash(blockhashString)
          .toFutureOr
      block <- xsnService
          .getBlock(blockhash)
          .toFutureOr

      rewards <- getBlockRewards(block).toFutureOr
    } yield BlockDetails(block, rewards)

    result.toFuture
  }

  def getDetails(height: Height): FutureApplicationResult[BlockDetails] = {
    val result = for {
      blockhash <- xsnService
          .getBlockhash(height)
          .toFutureOr

      block <- xsnService
          .getBlock(blockhash)
          .toFutureOr

      rewards <- getBlockRewards(block).toFutureOr
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

  private def getBlockRewards(block: Block): FutureApplicationResult[BlockRewards] = {
    if (block.isPoW) {
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
      vout <- transactionLogic.getVOUT(0, tx, BlockNotFoundError).toFutureOr
      address <- transactionLogic.getAddress(vout, BlockNotFoundError).toFutureOr
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
          .getVIN(coinstakeTx, BlockNotFoundError)
          .toFutureOr

      previousToCoinstakeTx <- xsnService
          .getTransaction(coinstakeTxVIN.txid)
          .toFutureOr
      previousToCoinstakeVOUT <- transactionLogic
          .getVOUT(coinstakeTxVIN, previousToCoinstakeTx, BlockNotFoundError)
          .toFutureOr

      coinstakeAddress <- transactionLogic
          .getAddress(previousToCoinstakeVOUT, BlockNotFoundError)
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
          .getVIN(coinstakeTx, BlockNotFoundError)
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
            .getVOUT(coinstakeTxVIN, previousToCoinstakeTx, BlockNotFoundError)
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
