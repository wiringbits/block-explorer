package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block

import scala.language.higherKinds

trait BlockDataHandler[F[_]] {

  def create(block: Block): F[Block]

  def getBy(blockhash: Blockhash): F[Block]
}

trait BlockBlockingDataHandler extends BlockDataHandler[ApplicationResult]
