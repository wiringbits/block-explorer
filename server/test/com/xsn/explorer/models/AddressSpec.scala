package com.xsn.explorer.models

import com.xsn.explorer.models.values._
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class AddressSpec extends WordSpec with MustMatchers with OptionValues {

  val addresses = Map(
    "xsn legacy" -> "19Gfgd95swmT8jmktPK19mbYw9hiBGYV4",
    "P2WPKH" -> "bc1qzhayf65p2j4h3pfw22aujgr5w42xfqzx5uvddt",
    "bech32" -> "xc1qphyjl73szcnz3jfpjryljx8elwc6wpdqqccy3s8g57gw7e44hw4q2jqdds",
    "btc address 1" -> "1111111111111111111114oLvT2",
    "btc weird one" -> "bc1zqyqsywvzqe"
  )

  "from" should {

    addresses.foreach {
      case (tpe, input) =>
        s"allow $tpe" in {
          val result = Address.from(input)
          result.value.string mustEqual input
        }
    }

    "reject an empty string" in {
      val input = ""
      val result = Address.from(input)
      result mustEqual Option.empty
    }

    "reject whitespaces" in {
      val input = " Xvjue2ZLnJwTrSLUBx7DTHaSHTdpWrxtLF "
      val result = Address.from(input)
      result mustEqual Option.empty
    }

    "reject invalid characters" in {
      val input = "bc1qzhayf65p2j4h3pfw22aujgr5w42xfqzx.uvddt"
      val result = Address.from(input)
      result mustEqual Option.empty
    }

    "reject invalid address, 1 character missing" in {
      pending

      val input = "Xvjue2ZLnJwTrSLUBx7DTHaSHTdpWrxtL"
      val result = Address.from(input)
      result mustEqual Option.empty
    }
  }
}
