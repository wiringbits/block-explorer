package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.data.async.{BalanceFutureDataHandler, TransactionFutureDataHandler}
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.models.persisted.{Balance, Transaction}
import com.xsn.explorer.models.Address
import javax.inject.Inject
import org.scalactic.{One, Or}

import scala.concurrent.ExecutionContext

class AddressService @Inject() (
    balanceFutureDataHandler: BalanceFutureDataHandler,
    transactionFutureDataHandler: TransactionFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def getBy(addressString: String): FutureApplicationResult[Balance] = {
    val result = for {
      address <- getAddress(addressString).toFutureOr
      balance <- balanceFutureDataHandler.getBy(address).toFutureOr
    } yield balance

    result.toFuture
  }

  def getUnspentOutputs(addressString: String): FutureApplicationResult[List[Transaction.Output]] = {
    val result = for {
      address <- getAddress(addressString).toFutureOr
      outputs <- transactionFutureDataHandler.getUnspentOutputs(address).toFutureOr
    } yield outputs

    result.toFuture
  }

  private def getAddress(addressString: String): ApplicationResult[Address] = {
    val maybe = Address.from(addressString)
    Or.from(maybe, One(AddressFormatError))
  }
}
