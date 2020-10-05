package com.xsn.explorer.services

import com.xsn.explorer.errors.{BlockNotFoundError, TransactionError}
import com.xsn.explorer.helpers.BlockLoader
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.validators.{AddressValidator, TransactionIdValidator}
import org.mockito.MockitoSugar._
import org.scalactic.{Bad, Good, One}
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransactionRPCServiceSpec extends AnyWordSpec {
  val transactionIdValidator = mock[TransactionIdValidator]
  val transactionCollectorService = mock[TransactionCollectorService]
  val xsnService = mock[XSNService]
  val addresValidator = new AddressValidator()

  val service =
    new TransactionRPCService(transactionIdValidator, transactionCollectorService, xsnService)

  "getTransactionLite(height, txindex)" should {
    "get non-cacheable lite transaction" in {
      val height = Height(123)
      val blockhash = Blockhash.from("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32").get
      val hexTransaction = "000006a473044022054b5726d41608f8a5d587b4908d3d1f2835625a5d48c819a20ec8f7b83e"
      val txindex = 0

      val block = BlockLoader.getRPC(blockhash.string)
      val rawTransaction = Json.obj("hex" -> hexTransaction)

      when(xsnService.getLatestBlock())
        .thenReturn(Future.successful(Good(block)))

      when(xsnService.getBlockhash(height))
        .thenReturn(Future.successful(Good(blockhash)))

      when(xsnService.getBlock(blockhash))
        .thenReturn(Future.successful(Good(block)))

      when(xsnService.getRawTransaction(block.transactions(txindex)))
        .thenReturn(Future.successful(Good(rawTransaction)))

      whenReady(service.getTransactionLite(height, txindex)) { result =>
        result.isGood mustEqual true

        val expectedResult = Json.obj(
          "hex" -> hexTransaction,
          "blockhash" -> blockhash.string
        )
        result.get._1 mustBe expectedResult
        result.get._2 mustBe false
      }
    }

    "get cacheable lite transaction" in {
      val height = Height(123)
      val blockhash = Blockhash.from("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32").get
      val hexTransaction = "000006a473044022054b5726d41608f8a5d587b4908d3d1f2835625a5d48c819a20ec8f7b83e"
      val txindex = 0

      val block = BlockLoader.getRPC(blockhash.string)
      val rawTransaction = Json.obj("hex" -> hexTransaction)

      when(xsnService.getLatestBlock())
        .thenReturn(Future.successful(Good(block.copy(height = Height(150)))))

      when(xsnService.getBlockhash(height))
        .thenReturn(Future.successful(Good(blockhash)))

      when(xsnService.getBlock(blockhash))
        .thenReturn(Future.successful(Good(block)))

      when(xsnService.getRawTransaction(block.transactions(txindex)))
        .thenReturn(Future.successful(Good(rawTransaction)))

      whenReady(service.getTransactionLite(height, txindex)) { result =>
        result.isGood mustEqual true

        val expectedResult = Json.obj(
          "hex" -> hexTransaction,
          "blockhash" -> blockhash.string
        )
        result.get._1 mustBe expectedResult
        result.get._2 mustBe true
      }
    }

    "fail when transaction with txid does not exists" in {
      val height = Height(123)
      val blockhash = Blockhash.from("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32").get
      val txindex = 999

      val block = BlockLoader.getRPC(blockhash.string)

      when(xsnService.getLatestBlock())
        .thenReturn(Future.successful(Good(block)))

      when(xsnService.getBlockhash(height))
        .thenReturn(Future.successful(Good(blockhash)))

      when(xsnService.getBlock(blockhash))
        .thenReturn(Future.successful(Good(block)))

      whenReady(service.getTransactionLite(height, txindex)) { result =>
        result mustBe Bad(One(TransactionError.IndexNotFound(height, txindex)))
      }
    }

    "fail block does not exists" in {
      val height = Height(123)
      val txindex = 0

      when(xsnService.getLatestBlock())
        .thenReturn(Future.successful(Bad(One(BlockNotFoundError))))

      whenReady(service.getTransactionLite(height, txindex)) { result =>
        result mustBe Bad(One(BlockNotFoundError))
      }
    }
  }
}
