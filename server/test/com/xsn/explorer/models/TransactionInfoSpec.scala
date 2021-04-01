package com.xsn.explorer.models

import com.xsn.explorer.helpers.DataGenerator
import com.xsn.explorer.models.values._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TransactionInfoSpec extends AnyWordSpec with Matchers with DataGenerator {
  "fee" should {
    "calculate the paid fee" in {
      val transactionInfo = TransactionInfo(
        randomTransactionId,
        randomBlockhash,
        0L,
        Size(0),
        BigDecimal(125),
        BigDecimal(25),
        Height(0)
      )

      transactionInfo.fee mustBe BigDecimal(100)
    }

    "return 0 when result would be negative" in {
      val transactionInfo = TransactionInfo(
        randomTransactionId,
        randomBlockhash,
        0L,
        Size(0),
        BigDecimal(25),
        BigDecimal(125),
        Height(0)
      )

      transactionInfo.fee mustBe BigDecimal(0)
    }
  }
}
