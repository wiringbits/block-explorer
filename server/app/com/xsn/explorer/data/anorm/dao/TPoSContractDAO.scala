package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.TPoSContractParsers
import com.xsn.explorer.models.TPoSContract
import com.xsn.explorer.models.values.{Address, TransactionId}

class TPoSContractDAO {

  import TPoSContractParsers._

  def create(contract: TPoSContract)(implicit conn: Connection): TPoSContract = {
    SQL(
      """
        |INSERT INTO tpos_contracts
        |  (txid, index, owner, merchant, merchant_commission, state, time)
        |VALUES
        |  ({txid}, {index}, {owner}, {merchant}, {merchant_commission}, {state}::TPOS_CONTRACT_STATE, {time})
        |RETURNING txid, index, owner, merchant, merchant_commission, state, time
      """.stripMargin
    ).on(
        'txid -> contract.id.txid.string,
        'index -> contract.id.index,
        'owner -> contract.details.owner.string,
        'merchant -> contract.details.merchant.string,
        'merchant_commission -> contract.details.merchantCommission.int,
        'state -> contract.state.entryName,
        'time -> contract.time
      )
      .as(parseTPoSContract.single)
  }

  def deleteBy(txid: TransactionId)(implicit conn: Connection): Option[TPoSContract] = {
    SQL(
      """
        |DELETE FROM tpos_contracts
        |WHERE txid = {txid}
        |RETURNING txid, index, owner, merchant, merchant_commission, state, time
      """.stripMargin
    ).on(
        'txid -> txid.string
      )
      .as(parseTPoSContract.singleOpt)
  }

  def close(id: TPoSContract.Id, closedOn: TransactionId)(implicit conn: Connection): Unit = {
    val _ = SQL(
      """
        |UPDATE tpos_contracts
        |SET state = {state}::TPOS_CONTRACT_STATE,
        |    closed_on = {closed_on}
        |WHERE txid = {txid} AND
        |      index = {index}
      """.stripMargin
    ).on(
        'txid -> id.txid.string,
        'index -> id.index,
        'state -> TPoSContract.State.Closed.entryName,
        'closed_on -> closedOn.string
      )
      .executeUpdate()
  }

  def open(id: TPoSContract.Id)(implicit conn: Connection): Unit = {
    val _ = SQL(
      """
        |UPDATE tpos_contracts
        |SET state = {state}::TPOS_CONTRACT_STATE,
        |    closed_on = null
        |WHERE txid = {txid} AND
        |      index = {index}
      """.stripMargin
    ).on(
        'txid -> id.txid.string,
        'index -> id.index,
        'state -> TPoSContract.State.Active.entryName
      )
      .executeUpdate()
  }

  def getBy(address: Address)(implicit conn: Connection): List[TPoSContract] = {
    SQL(
      """
        |SELECT txid, index, owner, merchant, merchant_commission, state, time
        |FROM tpos_contracts
        |WHERE owner = {address} OR merchant = {address}
        |ORDER BY time DESC
      """.stripMargin
    ).on(
        'address -> address.string
      )
      .as(parseTPoSContract.*)
  }
}
