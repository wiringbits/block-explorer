package com.xsn.explorer.util

import com.xsn.explorer.models.persisted.{AddressTransactionDetails, Transaction}

object TransactionAddressesHelper {

  private def computeReceiveDetails(
      transaction: Transaction.HasIO
  ): Iterable[AddressTransactionDetails] = {
    val outputAddressValueList = for {
      output <- transaction.outputs
      address <- output.addresses
    } yield address -> output.value

    val received = outputAddressValueList
      .groupBy(_._1)
      .view
      .mapValues { _.map(_._2).sum }
      .toMap
      .map { case (address, value) =>
        AddressTransactionDetails(
          address,
          transaction.id,
          time = transaction.time,
          received = value
        )
      }

    received
  }

  private def computeSendDetails(
      transaction: Transaction.HasIO
  ): Iterable[AddressTransactionDetails] = {
    val inputAddressValueList = for {
      input <- transaction.inputs
      address <- input.addresses
    } yield address -> input.value

    val sent = inputAddressValueList
      .groupBy(_._1)
      .view
      .mapValues { _.map(_._2).sum }
      .toMap
      .map { case (address, value) =>
        AddressTransactionDetails(
          address,
          transaction.id,
          time = transaction.time,
          sent = value
        )
      }

    sent
  }

  def computeDetails(
      transaction: Transaction.HasIO
  ): Iterable[AddressTransactionDetails] = {
    val details =
      (computeReceiveDetails(transaction) ++ computeSendDetails(transaction))
        .groupBy(_.address)
        .view
        .mapValues { case head :: list =>
          list.foldLeft(head)(merge)
        }
        .toMap
        .values

    details
  }

  private def merge(
      x: AddressTransactionDetails,
      y: AddressTransactionDetails
  ): AddressTransactionDetails = {
    x.copy(received = x.received + y.received, sent = x.sent + y.sent)
  }
}
