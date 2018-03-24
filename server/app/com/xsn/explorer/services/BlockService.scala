package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.services.logic.{BlockLogic, TransactionLogic}

import scala.concurrent.ExecutionContext

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

  // TODO: Handle blocks with coin split
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

      previousToCoinstakeTx <- xsnService
          .getTransaction(coinstakeTxVIN.txid)
          .toFutureOr
      previousToCoinstakeVOUT <- transactionLogic
          .getVOUT(coinstakeTxVIN, previousToCoinstakeTx, BlockNotFoundError)
          .toFutureOr

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
          .getTPoSRewards(coinstakeTx, ownerAddress, merchantAddress, previousToCoinstakeVOUT.value)
          .toFutureOr
    } yield rewards

    result.toFuture
  }
}
