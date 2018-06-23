package com.xsn.explorer.data

import com.alexitc.playsonify.models._
import com.xsn.explorer.data.anorm.TransactionPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.TransactionPostgresDAO
import com.xsn.explorer.data.anorm.interpreters.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.TransactionNotFoundError
import com.xsn.explorer.helpers.DataHelper._
import com.xsn.explorer.models.fields.TransactionField
import com.xsn.explorer.models.{Size, Transaction, TransactionWithValues}
import org.scalactic.{Bad, Good}

class TransactionPostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new TransactionPostgresDataHandler(database, new TransactionPostgresDAO(new FieldOrderingSQLInterpreter))
  val defaultOrdering = FieldOrdering(TransactionField.Time, OrderingCondition.DescendingOrder)

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

  "deleteBy blockhash" should {
    "delete the transactions related to a block" in {
      dataHandler.upsert(transaction).isGood mustEqual true

      val result = dataHandler.deleteBy(transaction.blockhash)
      result.isGood mustEqual true
      result.get mustEqual List(transaction)
    }
  }

  "getBy address" should {
    val address = createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW86F")
    val inputs = List(
      Transaction.Input(0, None, None),
      Transaction.Input(1, Some(BigDecimal(100)), Some(address))
    )

    val outputs = List(
      Transaction.Output(0, BigDecimal(50), address, None, None),
      Transaction.Output(
        1,
        BigDecimal(150),
        createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
        None, None)
    )

    val transaction = Transaction(
      createTransactionId("92c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
      createBlockhash("ad22f0dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
      12312312L,
      Size(1000),
      inputs,
      outputs)

    val query = PaginatedQuery(Offset(0), Limit(10))

    "find no results" in {
      val expected = PaginatedResult(query.offset, query.limit, Count(0), List.empty)
      val result = dataHandler.getBy(address, query, defaultOrdering)

      result mustEqual Good(expected)
    }

    "find the right values" in {
      val transactionWithValues = TransactionWithValues(
        transaction.id, transaction.blockhash, transaction.time, transaction.size,
        sent = 100,
        received = 200)

      val expected = PaginatedResult(query.offset, query.limit, Count(1), List(transactionWithValues))
      dataHandler.upsert(transaction).isGood mustEqual true

      val result = dataHandler.getBy(address, query, defaultOrdering)
      result mustEqual Good(expected)
    }
  }
}
