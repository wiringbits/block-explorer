package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.models._
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
          .getRewards(coinstakeTx, coinstakeAddress, previousToCoinstakeVOUT.value)
          .toFutureOr
    } yield BlockDetails(block, rewards)

    result.toFuture
  }
}
