package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.data.async.{BalanceFutureDataHandler, TransactionFutureDataHandler}
import com.xsn.explorer.models.persisted.{Balance, Transaction}
import com.xsn.explorer.services.validators.AddressValidator
import javax.inject.Inject

import scala.concurrent.ExecutionContext

class AddressService @Inject() (
    addressValidator: AddressValidator,
    balanceFutureDataHandler: BalanceFutureDataHandler,
    transactionFutureDataHandler: TransactionFutureDataHandler
)(implicit ec: ExecutionContext) {

  def getBy(addressString: String): FutureApplicationResult[Balance] = {
    val result = for {
      address <- addressValidator.validate(addressString).toFutureOr
      balance <- balanceFutureDataHandler.getBy(address).toFutureOr
    } yield balance

    result.toFuture
  }

  def getUnspentOutputs(
      addressString: String
  ): FutureApplicationResult[List[Transaction.Output]] = {
    val result = for {
      address <- addressValidator.validate(addressString).toFutureOr
      outputs <- transactionFutureDataHandler
        .getUnspentOutputs(address)
        .toFutureOr
    } yield outputs

    result.toFuture
  }
}
