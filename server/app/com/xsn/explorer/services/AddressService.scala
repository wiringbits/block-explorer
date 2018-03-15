package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.models.{Address, AddressDetails}
import org.scalactic.{One, Or}

import scala.concurrent.ExecutionContext

class AddressService @Inject() (xsnService: XSNService)(implicit ec: ExecutionContext) {

  def getDetails(addressString: String): FutureApplicationResult[AddressDetails] = {
    val result = for {
      address <- {
        val maybe = Address.from(addressString)
        Or.from(maybe, One(AddressFormatError)).toFutureOr
      }

      balance <- xsnService.getAddressBalance(address).toFutureOr
      transactionCount <- xsnService.getTransactionCount(address).toFutureOr
    } yield AddressDetails(balance, transactionCount)

    result.toFuture
  }
}
