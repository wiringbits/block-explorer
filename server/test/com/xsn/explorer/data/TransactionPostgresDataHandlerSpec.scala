package com.xsn.explorer.data

import com.xsn.explorer.data.anorm.TransactionPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.TransactionPostgresDAO
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.TransactionNotFoundError
import com.xsn.explorer.helpers.DataHelper._
import com.xsn.explorer.models.{Size, Transaction}
import org.scalactic.{Bad, Good}

class TransactionPostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new TransactionPostgresDataHandler(database, new TransactionPostgresDAO)

  val inputs = List(
    Transaction.Input(0, None, None),
    Transaction.Input(1, Some(BigDecimal(100)), Some(createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F")))
  )

  val outputs = List(
    Transaction.Output(0, BigDecimal(50), createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F"), None, None),
    Transaction.Output(
      1,
      BigDecimal(150),
      createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
      Some(createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt")),
      Some(createAddress("XjfNeGJhLgW3egmsZqdbpCNGfysPs7jTNm")))
  )

  val transaction = Transaction(
    createTransactionId("99c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
    createBlockhash("ad92f0dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
    12312312L,
    Size(1000),
    inputs,
    outputs)

  "upsert" should {
    "add a new transaction" in {
      val result = dataHandler.upsert(transaction)
      result mustEqual Good(transaction)
    }

    "update an existing transaction" in {
      val newTransaction = transaction.copy(
        blockhash = createBlockhash("99c51e4fe89466faa734d6207a7ef6115fa1dd32f7156b006fafc6bb85a79eb8"),
        time = 2313121L,
        size = Size(2000))

      dataHandler.upsert(transaction).isGood mustEqual true
      val result = dataHandler.upsert(newTransaction)
      result mustEqual Good(newTransaction)
    }
  }

  "delete" should {
    "delete a transaction" in {
      dataHandler.upsert(transaction).isGood mustEqual true
      val result = dataHandler.delete(transaction.id)
      result mustEqual Good(transaction)
    }

    "fail to delete a non-existent transaction" in {
      dataHandler.delete(transaction.id)
      val result = dataHandler.delete(transaction.id)
      result mustEqual Bad(TransactionNotFoundError).accumulating
    }
  }
}
