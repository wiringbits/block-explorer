package com.xsn.explorer.modules

import com.google.inject.AbstractModule
import com.xsn.explorer.data._
import com.xsn.explorer.data.anorm._
import com.xsn.explorer.services.synchronizer.repository.{BlockChunkPostgresRepository, BlockChunkRepository}

class DataHandlerModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[BlockBlockingDataHandler]).to(classOf[BlockPostgresDataHandler])
    bind(classOf[BalanceBlockingDataHandler]).to(classOf[BalancePostgresDataHandler])
    bind(classOf[StatisticsBlockingDataHandler]).to(classOf[StatisticsPostgresDataHandler])
    bind(classOf[TransactionBlockingDataHandler]).to(classOf[TransactionPostgresDataHandler])
    bind(classOf[LedgerBlockingDataHandler]).to(classOf[LedgerPostgresDataHandler])
    bind(classOf[TPoSContractBlockingDataHandler]).to(classOf[TPoSContractPostgresDataHandler])
    bind(classOf[BlockChunkRepository.Blocking]).to(classOf[BlockChunkPostgresRepository])
  }
}
