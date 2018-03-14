package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.TransactionFormatError
import com.xsn.explorer.models.{TransactionDetails, TransactionId}
import org.scalactic.{Good, One, Or}

import scala.concurrent.{ExecutionContext, Future}

class TransactionService @Inject() (xsnService: XSNService)(implicit ec: ExecutionContext) {

  def getTransaction(txidString: String): FutureApplicationResult[TransactionDetails] = {
    val result = for {
      txid <- {
        val maybe = TransactionId.from(txidString)
        Or.from(maybe, One(TransactionFormatError)).toFutureOr
      }

      transaction <- xsnService.getTransaction(txid).toFutureOr

      previousMaybe <- transaction
          .vin
          .map(_.txid)
          .map(xsnService.getTransaction)
          .map { f => f.toFutureOr.map(Option.apply).toFuture }
          .getOrElse { Future.successful(Good(Option.empty))}
          .toFutureOr
    } yield {
      previousMaybe
          .map { previous => TransactionDetails.from(transaction, previous) }
          .getOrElse { TransactionDetails.from(transaction) }
    }

    result.toFuture
  }
}
