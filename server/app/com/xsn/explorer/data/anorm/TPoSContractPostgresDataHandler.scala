package com.xsn.explorer.data.anorm

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.TPoSContractBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.TPoSContractDAO
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.values.Address
import javax.inject.Inject
import org.scalactic.Good
import play.api.db.Database

class TPoSContractPostgresDataHandler @Inject() (
    override val database: Database,
    tposContractDAO: TPoSContractDAO
) extends TPoSContractBlockingDataHandler
    with AnormPostgresDataHandler {

  def getBy(address: Address): ApplicationResult[List[TPoSContract]] =
    withConnection { implicit conn =>
      val result = tposContractDAO.getBy(address)
      Good(result)
    }
}
