package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.HexString
import com.xsn.explorer.models.values._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class ScriptPubKeySpec extends WordSpec with MustMatchers with OptionValues {

  private val dummyScript = HexString.from("00").get

  "getTPoSAddresses" should {

    "parse the addresses" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99", dummyScript, List.empty)
      val expected = (
          Address.from("Xi3sQfMQsy2CzMZTrnKW6HFGp1VqFThdLw").get,
          Address.from("XyJC8xnfFrHNcMinh6gxuPRYY9HCaY9DAo").get)

      val result = script.getTPoSAddresses
      result.value mustEqual expected
    }

    "support more than 4 values if we have the addresses" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 586a55587938507a55464d78534c37594135767866574a587365746b354d5638676f 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99 1f60a6a385a4e5163ffef65dd873f17452bb0d9f89da701ffcc5a0f72287273c0571485c29123fef880d2d8169cfdb884bf95a18a0b36461517acda390ce4cf441", dummyScript, List.empty)

      val result = script.getTPoSAddresses
      result.nonEmpty mustEqual true
    }

    "fail if OP_RETURN is not present" in {
      val script = ScriptPubKey("nulldata", "OP_RTURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99", dummyScript, List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }

    "fail if the comission is missing" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f ", dummyScript, List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }

    "fail if the owner address is malformed" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 586933735164d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99", dummyScript, List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }

    "fail if the merchant address is malformed" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786664672484e634d696e68366778755052595939484361593944416f 99", dummyScript, List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }
  }
}
