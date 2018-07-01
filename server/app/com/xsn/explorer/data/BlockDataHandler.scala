package com.xsn.explorer.data

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.{Blockhash, Height}

import scala.language.higherKinds

trait BlockDataHandler[F[_]] {

  def getBy(blockhash: Blockhash): F[Block]

  def getBy(height: Height): F[Block]

  def delete(blockhash: Blockhash): F[Block]

  def getLatestBlock(): F[Block]

  def getFirstBlock(): F[Block]
}

trait BlockBlockingDataHandler extends BlockDataHandler[ApplicationResult]
