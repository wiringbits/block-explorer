package com.xsn.explorer.data

import com.xsn.explorer.data.anorm.TPoSContractPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.TPoSContractDAO
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.helpers.DataGenerator
import com.xsn.explorer.models.TPoSContract

@com.github.ghik.silencer.silent
class TPoSContractPostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  val dao = new TPoSContractDAO
  lazy val dataHandler = new TPoSContractPostgresDataHandler(database, dao)

  "getBy" should {
    "return the contracts matching the owner or the merchant address" in {
      val owner = DataGenerator.randomAddress
      val merchant = DataGenerator.randomAddress
      pending
    }
  }

  private def create(contract: TPoSContract): Unit = {
    val _ = database.withConnection { implicit conn =>
      dao.create(contract)
    }
  }
}
