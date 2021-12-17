package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureListOps, FutureOps}
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.data.async.TransactionFutureDataHandler
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.gcs.{GolombCodedSet, GolombEncoding}
import com.xsn.explorer.models._
import com.xsn.explorer.models.values._
import io.scalaland.chimney.dsl._

import javax.inject.Inject
import org.scalactic.{Bad, Good, One, Or}

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

class TransactionCollectorService @Inject() (
    xsnService: XSNService,
    transactionDataHandler: TransactionFutureDataHandler
)(implicit ec: ExecutionContext) {

  import TransactionCollectorService._

  /** Collects the given transactions, returning all or none.
    *
    * The method is designed for the block synchronization process, hence, we assume that the transactions aren't in the
    * database but the reference transactions might be in the database.
    *
    * For each transaction, it is retrieved from the RPC API in parallel, trying sequentially if the RPC server gets
    * overloaded.
    *
    * When retrieving a transaction from the RPC API, the inputs are completed from the first available source:
    *   - The database.
    *   - The RPC API.
    *
    * When dealing with the RPC API, we try to get the details in parallel, if the server gets overloaded, we'll try to
    * load the data sequentially.
    */
  def collect(block: rpc.Block[_]): FutureApplicationResult[Result] = {
    val futureOr = for {
      rpcTransactions <- getRPCTransactions(block).toFutureOr
      completeTransactions <- completeValues(rpcTransactions).toFutureOr
      result = completeTransactions.map(persisted.Transaction.fromRPC)
      potentialTposTransactions = result.collect { case (transaction, true) =>
        transaction
      }
      validTposTransactions <- getValidContracts(
        potentialTposTransactions
      ).toFutureOr
      contracts <- getTposContracts(validTposTransactions).toFutureOr

    } yield {
      val completeBlock = block
        .into[rpc.Block.HasTransactions[rpc.TransactionVIN.HasValues]]
        .withFieldConst(_.transactions, completeTransactions)
        .transform

      val txs = result.map(_._1)
      (txs, contracts, () => GolombEncoding.encode(completeBlock))
    }

    futureOr.toFuture
  }

  private[services] def completeValues(
      rpcTransactions: List[RPCTransaction]
  ): FutureApplicationResult[List[RPCCompleteTransaction]] = {
    val neutral: FutureApplicationResult[
      List[rpc.Transaction[rpc.TransactionVIN.HasValues]]
    ] =
      Future.successful(Good(List.empty))
    val future = rpcTransactions.foldLeft(neutral) { case (acc, tx) =>
      val result = for {
        previous <- acc.toFutureOr
        completeVIN <- getRPCTransactionVIN(tx.vin).toFutureOr
        completeTX = tx.copy(vin = completeVIN)
      } yield completeTX :: previous

      result.toFuture
    }

    // keep original ordering
    future.toFutureOr
      .map(_.reverse)
      .toFuture
  }

  /** Retrieves the VIN details (address, amount, all or nothing):
    *   - Tries to retrieve the details from all inputs concurrently from the database.
    *   - The inputs that aren't present in the database are retrieved from the RPC API.
    *   - The ones that weren't retrieved are retried sequentially using the RPC API.
    */
  private[services] def getRPCTransactionVIN(
      vinList: List[rpc.TransactionVIN]
  ): FutureApplicationResult[List[rpc.TransactionVIN.HasValues]] = {
    getDBPartialVINList(vinList)
      .flatMap(completeRPCVINSequentially)
  }

  private[services] def getDBPartialVINList(
      vinList: List[rpc.TransactionVIN]
  ): Future[List[PartialTransactionVIN]] = {
    val futures = for (vin <- vinList) yield {
      transactionDataHandler
        .getOutput(vin.txid, vin.voutIndex)
        .toFutureOr
        .map { output =>
          vin.withValues(
            value = output.value,
            addresses = output.addresses,
            output.script
          )
        }
        .toFuture
        .map(vin -> _)
    }

    Future.sequence(futures)
  }

  private[services] def completeRPCVINSequentially(
      partial: List[PartialTransactionVIN]
  ): FutureApplicationResult[List[rpc.TransactionVIN.HasValues]] = {
    val neutral: FutureApplicationResult[List[rpc.TransactionVIN.HasValues]] =
      Future.successful(Good(List.empty))
    val result = partial.foldLeft(neutral) {
      case (acc, (vin, Bad(_))) =>
        val result = for {
          ready <- acc.toFutureOr
          newVIN <- getRPCTransactionVINWithValues(vin).toFutureOr
        } yield newVIN :: ready

        result.toFuture

      case (acc, (_, Good(newVIN))) =>
        val result = for {
          ready <- acc.toFutureOr
        } yield newVIN :: ready

        result.toFuture
    }

    result.toFutureOr
      .map(_.reverse)
      .toFuture
  }

  // TODO: Fix me, compiler problem due to type erasure
  @nowarn
  private[services] def getRPCTransactions(
      block: rpc.Block[_]
  ): FutureApplicationResult[List[RPCTransaction]] = {
    block match {
      case b: rpc.Block.Canonical =>
        getRPCTransactions(b.transactions)

      case b: rpc.Block.HasTransactions[rpc.TransactionVIN] =>
        Future.successful(Good(b.transactions))
    }
  }

  /** Get a list of RPC transactions (all or nothing):
    *   - Try to get them all from the RPC API in parallel
    *   - Retry the ones that weren't retrieved in parallel by retrieving them sequentially.
    */
  private[services] def getRPCTransactions(
      txidList: List[TransactionId]
  ): FutureApplicationResult[List[RPCTransaction]] = {
    getPartialRPCTransactions(txidList)
      .flatMap(completeRPCTransactionsSequentially)
  }

  private[services] def getPartialRPCTransactions(
      txidList: List[TransactionId]
  ): Future[List[PartialRPCTransaction]] = {
    val futures = for (txid <- txidList) yield {
      for {
        r <- xsnService.getTransaction(txid)
      } yield txid -> r
    }

    Future.sequence(futures)
  }

  private[services] def completeRPCTransactionsSequentially(
      partial: List[PartialRPCTransaction]
  ): FutureApplicationResult[List[RPCTransaction]] = {
    val neutral: FutureApplicationResult[List[RPCTransaction]] =
      Future.successful(Good(List.empty))
    val result = partial.foldLeft(neutral) {
      case (acc, (txid, Bad(_))) =>
        val result = for {
          ready <- acc.toFutureOr
          tx <- xsnService.getTransaction(txid).toFutureOr
        } yield tx :: ready

        result.toFuture

      case (acc, (_, Good(tx))) =>
        val result = for {
          ready <- acc.toFutureOr
        } yield tx :: ready

        result.toFuture
    }

    result.toFutureOr
      .map(_.reverse)
      .toFuture
  }

  /** Retrieves the values for the given VIN from the RPC API.
    */
  private[services] def getRPCTransactionVINWithValues(
      vin: rpc.TransactionVIN
  ): FutureApplicationResult[rpc.TransactionVIN.HasValues] = {
    val transactionValue = for {
      txResult <- xsnService.getTransaction(vin.txid)
    } yield txResult.flatMap { tx =>
      val maybe = tx.vout
        .find(_.n == vin.voutIndex)
        .flatMap(TransactionValue.from)

      Or.from(maybe, One(TransactionError.OutputNotFound(tx.id, vin.voutIndex)))
    }

    transactionValue.map {
      case Good(value) =>
        val newVIN = vin.withValues(
          value = value.value,
          addresses = value.addresses,
          value.pubKeyScript
        )
        Good(newVIN)

      case Bad(e) => Bad(e)
    }
  }

  private[services] def getTposContracts(
      transactions: List[persisted.Transaction.HasIO]
  ): FutureListOps[TPoSContract] = {

    val futures = transactions.map { transaction =>
      val result = for {
        contractDetails <- xsnService
          .getTPoSContractDetails(transaction.id)
          .toFutureOr

      } yield {
        val collateralMaybe = transaction.outputs.find(_.value == 1)
        val id = collateralMaybe
          .map(x => TPoSContract.Id(transaction.id, x.index))
          .getOrElse(
            throw new RuntimeException(
              s"The transaction ${transaction.id} is not a valid contract, missing collateral ${transaction.outputs}"
            )
          )
        TPoSContract(
          id,
          contractDetails,
          transaction.time,
          TPoSContract.State.Active
        )
      }

      result.toFuture
    }

    FutureListOps(futures)
  }

  private[services] def getValidContracts(
      transactions: List[persisted.Transaction.HasIO]
  ): FutureApplicationResult[List[persisted.Transaction.HasIO]] = {
    val listF = transactions
      .map { transaction =>
        xsnService
          .isTPoSContract(transaction.id)
          .toFutureOr
          .map { valid =>
            if (valid) Some(transaction)
            else None
          }
          .toFuture
      }

    val futureList = Future.sequence(listF)
    futureList.map { list =>
      val x = list.flatMap {
        case Good(a) => a.map(Good(_))
        case Bad(e) => Some(Bad(e))
      }

      val initial: ApplicationResult[List[persisted.Transaction.HasIO]] =
        Good(List.empty)
      x.foldLeft(initial) { case (acc, cur) =>
        cur match {
          case Good(txid) => acc.map(txid :: _)
          case Bad(e) => acc.badMap(prev => prev ++ e)
        }
      }
    }
  }
}

object TransactionCollectorService {

  type Result = (
      List[persisted.Transaction.HasIO],
      List[TPoSContract],
      () => GolombCodedSet
  )

  private type RPCTransaction = rpc.Transaction[rpc.TransactionVIN]
  private type RPCCompleteTransaction =
    rpc.Transaction[rpc.TransactionVIN.HasValues]

  private type PartialTransactionVIN =
    (rpc.TransactionVIN, ApplicationResult[rpc.TransactionVIN.HasValues])
  private type PartialRPCTransaction =
    (TransactionId, ApplicationResult[RPCTransaction])

}
