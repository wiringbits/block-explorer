package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.data.async.TransactionFutureDataHandler
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.helpers.{DataGenerator, DummyXSNService, Executors}
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.{ScriptPubKey, Transaction, TransactionVIN}
import com.xsn.explorer.models.values._
import org.scalactic.{Bad, Good, One}
import org.scalatest.EitherValues._
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future

class TransactionCollectorServiceSpec extends WordSpec {

  import Executors.globalEC

  def create(
      xsnService: XSNService,
      transactionDataHandler: TransactionBlockingDataHandler
  ): TransactionCollectorService = {

    val futureDataHandler = new TransactionFutureDataHandler(transactionDataHandler)(Executors.databaseEC)
    new TransactionCollectorService(xsnService, futureDataHandler)
  }

  "getRPCTransactionVINWithValues" should {
    val txid = DataGenerator.randomTransactionId
    val outputIndex = 1
    val vin = rpc.TransactionVIN.Raw(txid, outputIndex)
    val address = DataGenerator.randomAddress

    "return the values" in {
      val expected = vin.withValues(100, address)
      val xsnService = new DummyXSNService {
        override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction[TransactionVIN]] = {
          val output = rpc.TransactionVOUT(100, outputIndex, Some(createScript(address)))
          val tx = createTransaction(txid, List(output))
          Future.successful(Good(tx))
        }
      }

      val service = create(xsnService, null)
      whenReady(service.getRPCTransactionVINWithValues(vin)) { result =>
        result.toEither.right.value must be(expected)
      }
    }

    "fail when the transaction doesn't have the referenced output" in {
      val xsnService = new DummyXSNService {
        override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction[TransactionVIN]] = {
          val output = rpc.TransactionVOUT(100, 1 + outputIndex, Some(createScript(address)))
          val tx = createTransaction(txid, List(output))
          Future.successful(Good(tx))
        }
      }

      val service = create(xsnService, null)
      whenReady(service.getRPCTransactionVINWithValues(vin)) { result =>
        result.toEither.left.value must be(One(TransactionError.OutputNotFound(txid, outputIndex)))
      }
    }

    "fail when the transaction doesn't exists" in {
      val xsnService = new DummyXSNService {
        override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction[TransactionVIN]] = {
          Future.successful(Bad(TransactionError.NotFound(txid)).accumulating)
        }
      }

      val service = create(xsnService, null)
      whenReady(service.getRPCTransactionVINWithValues(vin)) { result =>
        result.toEither.left.value must be(One(TransactionError.NotFound(txid)))
      }
    }
  }

  "completeRPCTransactionsSequentially" should {
    "do nothing on empty list" in {
      val service = create(null, null)
      whenReady(service.completeRPCTransactionsSequentially(List.empty)) { result =>
        result.toEither.right.value must be(empty)
      }
    }

    "do nothing when all transactions are loaded" in {
      val input = for (_ <- 1 to 10) yield {
        val txid = DataGenerator.randomTransactionId
        val tx = createTransaction(txid, List.empty)
        txid -> Good(tx)
      }

      val service = create(null, null)
      whenReady(service.completeRPCTransactionsSequentially(input.toList)) { result =>
        result.toEither.right.value must be(input.flatMap(_._2.toOption))
      }
    }

    "fail when a single tx can't be completed" in {
      val completed = for (_ <- 1 to 10) yield {
        val txid = DataGenerator.randomTransactionId
        val tx = createTransaction(txid, List.empty)
        txid -> Good(tx)
      }

      val pending = {
        val x = DataGenerator.randomTransactionId
        x -> Bad(TransactionError.NotFound(x)).accumulating
      }
      val input = (completed.take(5).toList ::: pending :: completed.drop(5).toList).reverse

      val xsnService = new DummyXSNService {
        override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction[TransactionVIN]] = {
          Future.successful(Bad(TransactionError.NotFound(txid)).accumulating)
        }
      }

      val service = create(xsnService, null)
      whenReady(service.completeRPCTransactionsSequentially(input)) { result =>
        result.toEither.left.value must be(One(pending._2.swap.get.head))
      }
    }

    "complete the missing transactions" in {
      val completed = for (_ <- 1 to 10) yield {
        val txid = DataGenerator.randomTransactionId
        val tx = createTransaction(txid, List.empty)
        txid -> Good(tx)
      }
      val firstHalf = completed.take(5).toList
      val secondHalf = completed.drop(5).toList

      val pending1 = {
        val x = DataGenerator.randomTransactionId
        x -> Bad(TransactionError.NotFound(x)).accumulating
      }
      val pending1Tx = createTransaction(pending1._1, List.empty)

      val pending2 = {
        val x = DataGenerator.randomTransactionId
        x -> Bad(TransactionError.NotFound(x)).accumulating
      }
      val pending2Tx = createTransaction(pending2._1, List.empty)

      val input = firstHalf ::: List(pending1) ::: secondHalf ::: List(pending2)
      val xsnService = new DummyXSNService {
        override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction[TransactionVIN]] = {
          if (txid == pending1._1) {
            Future.successful(Good(pending1Tx))
          } else if (txid == pending2._1) {
            Future.successful(Good(pending2Tx))
          } else {
            Future.successful(Bad(TransactionError.NotFound(txid)).accumulating)
          }
        }
      }

      val service = create(xsnService, null)
      whenReady(service.completeRPCTransactionsSequentially(input)) { result =>
        val expected = firstHalf.flatMap(_._2.toOption) ::: List(pending1Tx) ::: secondHalf.flatMap(_._2.toOption) ::: List(
          pending2Tx
        )
        result.toEither.right.value must be(expected)
      }
    }
  }

  "getRPCTransactions" should {
    "fallback to retrieving transactions sequentally" in {
      val tx = createTransaction(DataGenerator.randomTransactionId, List.empty)
      val pending = createTransaction(DataGenerator.randomTransactionId, List.empty)

      val xsnService: XSNService = new DummyXSNService {

        var ready = Set.empty[TransactionId]

        override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction[TransactionVIN]] = {
          if (txid == tx.id) {
            Future.successful(Good(tx))
          } else if (txid == pending.id) {
            if (ready contains txid) {
              Future.successful(Good(pending))
            } else {
              ready = ready + txid
              Future.successful(Bad(TransactionError.NotFound(txid)).accumulating)
            }
          } else {
            Future.successful(Bad(TransactionError.NotFound(txid)).accumulating)
          }
        }
      }

      val input = List(tx.id, pending.id)
      val service = create(xsnService, null)
      whenReady(service.getRPCTransactions(input)) { result =>
        val expected = List(tx, pending)
        result.toEither.right.value must be(expected)
      }
    }
  }

  "getRPCTransactionVIN" should {
    "work" in {
      pending
    }
  }

  "completeValues" should {
    "work" in {
      pending
    }
  }

  "collect" should {
    "work" in {
      pending
    }
  }

  def createScript(address: Address) = {
    ScriptPubKey("nulldata", "", HexString.from("00").get, List(address))
  }

  def createTransaction(txid: TransactionId, outputs: List[rpc.TransactionVOUT]) = {
    rpc.Transaction(
      id = txid,
      size = Size(100),
      blockhash = DataGenerator.randomBlockhash,
      time = 0L,
      blocktime = 0L,
      confirmations = Confirmations(0),
      vin = List.empty[rpc.TransactionVIN],
      vout = outputs
    )
  }
}
