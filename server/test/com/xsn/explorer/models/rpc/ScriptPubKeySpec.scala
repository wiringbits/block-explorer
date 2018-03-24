package com.xsn.explorer.models.rpc

import com.xsn.explorer.models.Address
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class ScriptPubKeySpec extends WordSpec with MustMatchers with OptionValues {

  "getTPoSAddresses" should {

    "parse the addresses" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99", List.empty)
      val expected = (
          Address.from("Xi3sQfMQsy2CzMZTrnKW6HFGp1VqFThdLw").get,
          Address.from("XyJC8xnfFrHNcMinh6gxuPRYY9HCaY9DAo").get)

      val result = script.getTPoSAddresses
      result.value mustEqual expected
    }

    "fail if OP_RETURN is not present" in {
      val script = ScriptPubKey("nulldata", "OP_RTURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99", List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }

    "fail if the comission is missing" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f ", List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }

    "fail if the owner address is malformed" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 586933735164d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99", List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }

    "fail if the merchant address is malformed" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786664672484e634d696e68366778755052595939484361593944416f 99", List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }


    "fail if there are more than 4 values" in {
      val script = ScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99 x", List.empty)

      val result = script.getTPoSAddresses
      result.isEmpty mustEqual true
    }
  }
}
