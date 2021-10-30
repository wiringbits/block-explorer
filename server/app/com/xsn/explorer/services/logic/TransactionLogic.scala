package com.xsn.explorer.services.logic

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ApplicationError
import com.xsn.explorer.models.rpc.{Transaction, TransactionVIN, TransactionVOUT}
import com.xsn.explorer.models.values.Address
import org.scalactic.{One, Or}

class TransactionLogic {

  def getAddress(
      vout: TransactionVOUT,
      error: ApplicationError
  ): ApplicationResult[Address] = {
    val maybe = vout.addresses.flatMap(_.headOption)
    Or.from(maybe, One(error))
  }

  def getVIN(
      tx: Transaction[TransactionVIN],
      error: ApplicationError
  ): ApplicationResult[TransactionVIN] = {
    val maybe = tx.vin.headOption
    Or.from(maybe, One(error))
  }

  def getVOUT(
      vin: TransactionVIN,
      previousTX: Transaction[_],
      error: ApplicationError
  ): ApplicationResult[TransactionVOUT] = {
    getVOUT(vin.voutIndex, previousTX, error)
  }

  def getVOUT(
      index: Int,
      previousTX: Transaction[_],
      error: ApplicationError
  ): ApplicationResult[TransactionVOUT] = {
    val maybe = previousTX.vout.find(_.n == index)
    Or.from(maybe, One(error))
  }
}
