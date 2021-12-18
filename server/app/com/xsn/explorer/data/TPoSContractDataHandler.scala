package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.values.Address

trait TPoSContractDataHandler[F[_]] {

  def getBy(address: Address): F[List[TPoSContract]]
}

trait TPoSContractBlockingDataHandler extends TPoSContractDataHandler[ApplicationResult]
