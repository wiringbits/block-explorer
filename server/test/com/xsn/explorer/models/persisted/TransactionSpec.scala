package com.xsn.explorer.models.persisted

import com.xsn.explorer.helpers.DataGenerator
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.ScriptPubKey
import com.xsn.explorer.models.values._
import javax.xml.bind.DatatypeConverter
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec

class TransactionSpec extends WordSpec {

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
        Transaction.HasIO(
          tx,
          inputs = List.empty,
          outputs = outputs)
      }

      val _ = Transaction.HasIO(
        tx,
        inputs = List.empty,
        outputs = outputs.map(_.copy(txid = tx.id)))
    }
  }

  "fromRPC" should {
    "discard outputs without address" in {
      val address = DataGenerator.randomAddress
      val hex = HexString.from("00").get
      val vout = List(
        rpc.TransactionVOUT(0, 1, None),
        rpc.TransactionVOUT(10, 2, Some(ScriptPubKey(
          "nulldata",
          "",
          hex,
          List(address)))),
      )

      val tx = rpc.Transaction[rpc.TransactionVIN.HasValues](
        id = DataGenerator.randomTransactionId,
        size = Size(200),
        blockhash = DataGenerator.randomBlockhash,
        time = 10L,
        blocktime = 10L,
        confirmations = Confirmations(10),
        vin = List.empty,
        vout = vout)

      val expected = Transaction.Output(tx.id, 2, 10, address, hex)
      val (result, _) = persisted.Transaction.fromRPC(tx)
      result.outputs must be(List(expected))
    }

    "extract the possible TPoS contracts" in {
      val address = DataGenerator.randomAddress
      val addressHex = DatatypeConverter.printHexBinary(address.string.getBytes)
      val contractASM = s"OP_RETURN $addressHex $addressHex 90 aabbccff"
      val script = ScriptPubKey("nulldata", contractASM, HexString.from("00").get, List.empty)
      val voutWithContract = rpc.TransactionVOUT(value = 0, n = 1, Some(script))
      val collateral = rpc.TransactionVOUT(
        n = 0,
        value = 1
      )
      val tx = rpc.Transaction(
        id = DataGenerator.randomTransactionId,
        size = Size(200),
        blockhash = DataGenerator.randomBlockhash,
        time = 10L,
        blocktime = 10L,
        confirmations = Confirmations(10),
        vin = List(),
        vout = List(collateral, voutWithContract)).copy[rpc.TransactionVIN.HasValues](vin = List.empty)

      val expected = TPoSContract(
        id = TPoSContract.Id(tx.id, collateral.n),
        time = tx.time,
        state = TPoSContract.State.Active,
        details = TPoSContract.Details(address, address, TPoSContract.Commission.from(10).get)
      )

      val (_, contract) = persisted.Transaction.fromRPC(tx)
      contract.value must be(expected)
    }
  }
}
