package com.xsn.explorer.models

import com.xsn.explorer.models.values.Address
import javax.xml.bind.DatatypeConverter
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._
import org.scalatest.WordSpec

@com.github.ghik.silencer.silent
class TPoSContractSpec extends WordSpec {

  val address1 = Address.from("Xi3sQfMQsy2CzMZTrnKW6HFGp1VqFThdLw").get
  val address2 = Address.from("XyJC8xnfFrHNcMinh6gxuPRYY9HCaY9DAo").get
  val address1Hex = DatatypeConverter.printHexBinary(address1.string.getBytes())
  val address2Hex = DatatypeConverter.printHexBinary(address2.string.getBytes())
  val commission = "99"

  val signature =
    "1f60a6a385a4e5163ffef65dd873f17452bb0d9f89da701ffcc5a0f72287273c0571485c29123fef880d2d8169cfdb884bf95a18a0b36461517acda390ce4cf441"

  val failureCases = Map(
    "fail when the signature is missing" -> s"OP_RETURN $address1Hex $address2Hex $commission",
    "fail when there is an extra field" -> s"OP_RETURN $address1Hex $address2Hex $commission $signature $signature",
    "fail if OP_RETURN is not present" -> s"OP_RTURN $address1Hex $address2Hex $commission $signature",
    "fail if the commission is missing" -> s"OP_RETURN $address1Hex $address2Hex  $signature",
    "fail if the commission is corrupted" -> s"OP_RETURN $address1Hex $address2Hex $commission$commission $signature",
    "fail if the commission is 0" -> s"OP_RETURN $address1Hex $address2Hex 0 $signature",
    "fail if the commission is 100" -> s"OP_RETURN $address1Hex $address2Hex 100 $signature",
    "fail if the owner address is malformed" -> s"OP_RETURN x$address1Hex $address2Hex $commission $signature",
    "fail if the merchant address is malformed" -> s"OP_RETURN x$address1Hex $address2Hex $commission $signature"
  )

  "parsing details" should {
    "succeed on a valid contract" in {
      val asm = s"OP_RETURN $address1Hex $address2Hex $commission $signature"

      val expected = TPoSContract.Details(
        owner = address1,
        merchant = address2,
        merchantCommission = TPoSContract.Commission.from(100 - commission.toInt).get
      )

      val result = TPoSContract.Details.fromOutputScriptASM(asm)
      result.value must be(expected)
    }

    failureCases.foreach {
      case (test, input) =>
        test in {
          val result = TPoSContract.Details.fromOutputScriptASM(input)
          result must be(empty)
        }
    }
  }
}
