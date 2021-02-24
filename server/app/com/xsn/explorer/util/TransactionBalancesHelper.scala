package com.xsn.explorer.util

import com.xsn.explorer.models.persisted.{Balance, Transaction}
import com.xsn.explorer.models.values.Address

object TransactionBalancesHelper {

  def computeSpendMap(
      transactions: List[Transaction.HasIO]
  ): Map[Address, BigDecimal] = {
    val addressValueList = for {
      tx <- transactions
      input <- tx.inputs
      address <- input.addresses
    } yield address -> input.value

    addressValueList
      .groupBy(_._1)
      .mapValues { list =>
        list.map(_._2).sum
      }
  }

  def computeReceiveMap(
      transactions: List[Transaction.HasIO]
  ): Map[Address, BigDecimal] = {
    val addressValueList = for {
      tx <- transactions
      output <- tx.outputs
      address <- output.addresses
    } yield address -> output.value

    addressValueList
      .groupBy(_._1)
      .mapValues { list =>
        list.map(_._2).sum
      }
  }

  def computeBalances(
      transactions: List[Transaction.HasIO]
  ): Iterable[Balance] = {
    val spentList = computeSpendMap(transactions).map { case (address, spent) =>
      Balance(address, spent = spent)
    }

    val receiveList = computeReceiveMap(transactions).map {
      case (address, received) =>
        Balance(address, received = received)
    }

    val result = (spentList ++ receiveList)
      .groupBy(_.address)
      .mapValues { _.reduce(mergeBalances) }
      .values

    result
  }

  private def mergeBalances(a: Balance, b: Balance): Balance = {
    Balance(
      a.address,
      spent = a.spent + b.spent,
      received = a.received + b.received
    )
  }
}
