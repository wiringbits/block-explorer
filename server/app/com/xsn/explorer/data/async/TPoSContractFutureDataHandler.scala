package com.xsn.explorer.data.async

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.{TPoSContractBlockingDataHandler, TPoSContractDataHandler}
import com.xsn.explorer.executors.DatabaseExecutionContext
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.values.Address
import javax.inject.Inject

import scala.concurrent.Future

class TPoSContractFutureDataHandler @Inject()(blockingDataHandler: TPoSContractBlockingDataHandler)(
    implicit ec: DatabaseExecutionContext
) extends TPoSContractDataHandler[FutureApplicationResult] {

  override def getBy(address: Address): FutureApplicationResult[List[TPoSContract]] = Future {
    blockingDataHandler.getBy(address)
  }
}
