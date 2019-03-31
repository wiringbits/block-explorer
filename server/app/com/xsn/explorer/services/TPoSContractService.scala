package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.xsn.explorer.data.async.TPoSContractFutureDataHandler
import com.xsn.explorer.models.{TPoSContract, WrappedResult}
import com.xsn.explorer.services.validators.AddressValidator
import javax.inject.Inject

import scala.concurrent.ExecutionContext

class TPoSContractService @Inject() (
    addressValidator: AddressValidator,
    tposContractFutureDataHandler: TPoSContractFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def getBy(addressString: String): FutureApplicationResult[WrappedResult[List[TPoSContract]]] = {
    val result = for {
      address <- addressValidator.validate(addressString).toFutureOr
      contracts <- tposContractFutureDataHandler.getBy(address).toFutureOr
    } yield WrappedResult(contracts)

    result.toFuture
  }
}
