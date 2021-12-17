package com.xsn.explorer.models.persisted

import com.xsn.explorer.helpers.{DataGenerator, TransactionLoader}
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.ScriptPubKey
import com.xsn.explorer.models.values._

import javax.xml.bind.DatatypeConverter
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import scala.annotation.nowarn

@nowarn
class TransactionSpec extends AnyWordSpec {

  "HasIO" should {
    "expect outputs matching the txid" in {
      val tx = Transaction(
        id = DataGenerator.randomTransactionId,
        blockhash = DataGenerator.randomBlockhash,
        size = Size(20),
        time = 100L
      )

      val outputs = DataGenerator.randomOutputs(2)
      intercept[RuntimeException] {
        Transaction.HasIO(tx, inputs = List.empty, outputs = outputs)
      }

      val _ = Transaction.HasIO(
        tx,
        inputs = List.empty,
        outputs = outputs.map(_.copy(txid = tx.id))
      )
    }
  }

  "received" should {
    "sum outputs values" in {
      val tx = Transaction(
        id = DataGenerator.randomTransactionId,
        blockhash = DataGenerator.randomBlockhash,
        size = Size(20),
        time = 100L
      )

      val outputs = DataGenerator.randomOutputs(2)
      val withIO = Transaction.HasIO(
        tx,
        inputs = List.empty,
        outputs = outputs.map(_.copy(txid = tx.id))
      )

      withIO.received mustBe outputs.map(_.value).sum
    }

    "return zero when there are no outputs" in {
      val tx = Transaction(
        id = DataGenerator.randomTransactionId,
        blockhash = DataGenerator.randomBlockhash,
        size = Size(20),
        time = 100L
      )

      val withIO = Transaction.HasIO(
        tx,
        inputs = List.empty,
        outputs = List.empty
      )

      withIO.received mustBe BigDecimal(0)
    }
  }

  "sent" should {
    "sum the inputs values" in {
      val tx = Transaction(
        id = DataGenerator.randomTransactionId,
        blockhash = DataGenerator.randomBlockhash,
        size = Size(20),
        time = 100L
      )

      val outputs = DataGenerator.randomOutputs(2)
      val inputs = DataGenerator.randomInputs(outputs)
      val withIO = Transaction.HasIO(
        tx,
        inputs = inputs,
        outputs = List.empty
      )

      withIO.sent mustBe inputs.map(_.value).sum
    }

    "return zero when there are no inputs" in {
      val tx = Transaction(
        id = DataGenerator.randomTransactionId,
        blockhash = DataGenerator.randomBlockhash,
        size = Size(20),
        time = 100L
      )

      val withIO = Transaction.HasIO(
        tx,
        inputs = List.empty,
        outputs = List.empty
      )

      withIO.sent mustBe BigDecimal(0)
    }
  }

  "fromRPC" should {
    "discard outputs without address" in {
      val address = DataGenerator.randomAddress
      val hex = HexString.from("00").get
      val vout = List(
        rpc.TransactionVOUT(0, 1, None),
        rpc.TransactionVOUT(
          10,
          2,
          Some(ScriptPubKey("nulldata", "", hex, List(address)))
        )
      )

      val tx = rpc.Transaction[rpc.TransactionVIN.HasValues](
        id = DataGenerator.randomTransactionId,
        size = Size(200),
        blockhash = DataGenerator.randomBlockhash,
        time = 10L,
        blocktime = 10L,
        confirmations = Confirmations(10),
        vin = List.empty,
        vout = vout
      )

      val expected = Transaction.Output(tx.id, 2, 10, address, hex)
      val (result, _) = persisted.Transaction.fromRPC(tx)
      result.outputs must be(List(expected))
    }

    "discard outputs with 0 value" in {
      val address = DataGenerator.randomAddress
      val hex = HexString.from("00").get
      val script = ScriptPubKey("nulldata", "", hex, List(address))

      val vout = List(
        rpc.TransactionVOUT(0, 1, Some(script)),
        rpc.TransactionVOUT(10, 2, Some(script))
      )

      val tx = rpc.Transaction[rpc.TransactionVIN.HasValues](
        id = DataGenerator.randomTransactionId,
        size = Size(200),
        blockhash = DataGenerator.randomBlockhash,
        time = 10L,
        blocktime = 10L,
        confirmations = Confirmations(10),
        vin = List.empty,
        vout = vout
      )

      val expected = Transaction.Output(tx.id, 2, 10, address, hex)
      val (result, _) = persisted.Transaction.fromRPC(tx)
      result.outputs must be(List(expected))
    }

    "discard inputs with 0 value" in {
      val transactionId = DataGenerator.randomTransactionId
      val address = DataGenerator.randomAddress
      val hex = HexString.from("00").get

      val vin = List(
        rpc.TransactionVIN.HasValues(
          transactionId,
          voutIndex = 0,
          value = 0,
          List(address),
          hex
        ),
        rpc.TransactionVIN.HasValues(
          transactionId,
          voutIndex = 1,
          value = 10,
          List(address),
          hex
        )
      )

      val tx = rpc.Transaction[rpc.TransactionVIN.HasValues](
        id = transactionId,
        size = Size(200),
        blockhash = DataGenerator.randomBlockhash,
        time = 10L,
        blocktime = 10L,
        confirmations = Confirmations(10),
        vin = vin,
        vout = List.empty
      )

      val expected = Transaction.Input(
        tx.id,
        fromOutputIndex = 1,
        index = 1,
        value = 10,
        List(address)
      )
      val (result, _) = persisted.Transaction.fromRPC(tx)
      result.inputs must be(List(expected))
    }

    "extract the possible TPoS contracts" in {
      val address = DataGenerator.randomAddress
      val addressHex = DatatypeConverter.printHexBinary(address.string.getBytes)
      val contractASM = s"OP_RETURN $addressHex $addressHex 90 aabbccff"
      val script = ScriptPubKey(
        "nulldata",
        contractASM,
        HexString.from("00").get,
        List.empty
      )
      val voutWithContract = rpc.TransactionVOUT(value = 0, n = 1, Some(script))
      val collateral = rpc.TransactionVOUT(
        n = 0,
        value = 1
      )
      val tx = rpc
        .Transaction(
          id = DataGenerator.randomTransactionId,
          size = Size(200),
          blockhash = DataGenerator.randomBlockhash,
          time = 10L,
          blocktime = 10L,
          confirmations = Confirmations(10),
          vin = List(),
          vout = List(collateral, voutWithContract)
        )
        .copy[rpc.TransactionVIN.HasValues](vin = List.empty)

      val expected = TPoSContract(
        id = TPoSContract.Id(tx.id, collateral.n),
        time = tx.time,
        state = TPoSContract.State.Active,
        details = TPoSContract.Details(
          address,
          address,
          TPoSContract.Commission.from(10).get
        )
      )

      val (_, contract) = persisted.Transaction.fromRPC(tx)
      contract must be(true)
    }

    "accept outputs with empty script" in {
      val rpcTx = TransactionLoader.getWithValues(
        "728e76d2d5f0513aabeca6fd7101878469052e39f42f7a9b979a63d7e87353c2"
      )

      val expected = List(
        Transaction.Output(
          rpcTx.id,
          0,
          5.60400000,
          Address.from("Xgg7QHQcsBPYygYPj1QhDcCmfKXa7sFd9b").get,
          HexString
            .from(
              "2102a9f34e4bc8b77f6f99aab03646da75d770e4088fd38f1604e2b0f106e7314156ac"
            )
            .get
        ),
        Transaction.Output(
          rpcTx.id,
          1,
          1.40100000,
          List.empty,
          HexString.from("").get
        )
      )

      val tx = Transaction.fromRPC(rpcTx)._1
      tx.outputs must be(expected)
    }
  }
}
