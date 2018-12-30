package com.xsn.explorer.data

import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination._
import com.alexitc.playsonify.sql.FieldOrderingSQLInterpreter
import com.xsn.explorer.data.anorm.dao.{AggregatedAmountPostgresDAO, BalancePostgresDAO, BlockPostgresDAO, TransactionPostgresDAO}
import com.xsn.explorer.data.anorm.{BlockPostgresDataHandler, LedgerPostgresDataHandler, TransactionPostgresDataHandler}
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.{BlockNotFoundError, TransactionNotFoundError}
import com.xsn.explorer.helpers.DataHelper._
import com.xsn.explorer.helpers.{BlockLoader, TransactionLoader}
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import com.xsn.explorer.models.rpc.Block
import org.scalactic.{Every, Good, One, Or}
import org.scalatest.BeforeAndAfter

class TransactionPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  lazy val dataHandler = new TransactionPostgresDataHandler(database, new TransactionPostgresDAO(new FieldOrderingSQLInterpreter))
  lazy val ledgerDataHandler = new LedgerPostgresDataHandler(
    database,
    new BlockPostgresDAO(new FieldOrderingSQLInterpreter),
    new TransactionPostgresDAO(new FieldOrderingSQLInterpreter),
    new BalancePostgresDAO(new FieldOrderingSQLInterpreter),
    new AggregatedAmountPostgresDAO)

  lazy val blockDataHandler = new BlockPostgresDataHandler(database, new BlockPostgresDAO(new FieldOrderingSQLInterpreter))
  val defaultOrdering = FieldOrdering(TransactionField.Time, OrderingCondition.DescendingOrder)

  val block = Block(
    hash = createBlockhash("ad92f0dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
    previousBlockhash = None,
    nextBlockhash = None,
    merkleRoot = createBlockhash("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
    transactions = List.empty,
    confirmations = Confirmations(0),
    size = Size(10),
    height = Height(0),
    version = 0,
    time = 0,
    medianTime = 0,
    nonce = 0,
    bits = "abcdef",
    chainwork = "abcdef",
    difficulty = 12.2,
    tposContract = None
  )

  val dummyTransaction = Transaction(
    createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
    block.hash,
    12312312L,
    Size(1000),
    List.empty,
    List(
      Transaction.Output(createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"), 0, 1000, createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7Dkpss"), HexString.from("00").get, None, None),
      Transaction.Output(createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"), 1, 1000, createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7Dkpss"), HexString.from("00").get, None, None)
    )
  )

  val inputs = List(
    Transaction.Input(dummyTransaction.id, 0, 1, BigDecimal(100), createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F"))
  )

  val outputs = List(
    Transaction.Output(createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"), 0, BigDecimal(50), createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW85F"), HexString.from("00").get, None, None),
    Transaction.Output(
      createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
      1,
      BigDecimal(150),
      createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
      HexString.from("00").get,
      Some(createAddress("XfAATXtkRgCdMTrj2fxHvLsKLLmqAjhEAt")),
      Some(createAddress("XjfNeGJhLgW3egmsZqdbpCNGfysPs7jTNm")))
  )

  val transaction = Transaction(
    createTransactionId("99c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
    block.hash,
    12312312L,
    Size(1000),
    inputs,
    outputs.map(_.copy(txid = createTransactionId("99c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"))))

  val blockList = List(
    BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34"),
    BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7"),
    BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8"),
    BlockLoader.get("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd"),
    BlockLoader.get("00000b59875e80b0afc6c657bc5318d39e03532b7d97fb78a4c7bd55c4840c32"),
    BlockLoader.get("00000267225f7dba55d9a3493740e7f0dde0f28a371d2c3b42e7676b5728d020")
  )

  private def prepareBlock(block: Block) = {
    val dao = new BlockPostgresDAO(new FieldOrderingSQLInterpreter)
    try {
      database.withConnection { implicit conn =>
        val maybe = dao.insert(block)
        Or.from(maybe, One(BlockNotFoundError))
      }
    } catch {
      case _ => ()
    }
  }

  private def prepareTransaction(transaction: Transaction) = {
    try {
      upsertTransaction(transaction)
    } catch {
      case _ => ()
    }
  }

  private def upsertTransaction(transaction: Transaction) = {
    val dao = new TransactionPostgresDAO(new FieldOrderingSQLInterpreter)
    database.withConnection { implicit conn =>
      val maybe = dao.upsert(1, transaction)
      Or.from(maybe, One(TransactionNotFoundError))
    }
  }

  before {
    clearDatabase()
    prepareBlock(block)
    prepareTransaction(dummyTransaction)
  }

  "getBy address" should {
    val address = createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW86F")
    val inputs = List(
      Transaction.Input(dummyTransaction.id, 0, 1, 100, address),
      Transaction.Input(dummyTransaction.id, 1, 2, 200, createAddress("XxQ7j37LfuXgsLD5DZAwFKhT3s2ZMkW86F"))
    )

    val outputs = List(
      Transaction.Output(createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"), 0, BigDecimal(50), address, HexString.from("00").get, None, None),
      Transaction.Output(
        createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
        1,
        BigDecimal(250),
        createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
        HexString.from("00").get,
        None, None)
    )

    val transaction = Transaction(
      createTransactionId("92c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
      block.hash,
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
        received = 50)

      val expected = PaginatedResult(query.offset, query.limit, Count(1), List(transactionWithValues))
      upsertTransaction(transaction).isGood mustEqual true

      val result = dataHandler.getBy(address, query, defaultOrdering)
      result mustEqual Good(expected)
    }
  }

  "getUnspentOutputs" should {
    "return non-zero results" in {
      clearDatabase()
      val blocks = blockList.take(3)
      blocks.map(createBlock)

      val expected = Transaction.Output(
        address = createAddress("XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9"),
        txid = createTransactionId("67aa0bd8b9297ca6ee25a1e5c2e3a8dbbcc1e20eab76b6d1bdf9d69f8a5356b8"),
        index = 0,
        value = BigDecimal(76500000),
        script = HexString.from("2103e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029ac").get,
        tposMerchantAddress = None,
        tposOwnerAddress = None
      )

      val result = dataHandler.getUnspentOutputs(expected.address).get
      result.size mustEqual 1

      result mustEqual List(expected)
    }
  }

  "getByBlockhash" should {
    val blockhash = createBlockhash("0000000000bdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
    val inputs = List(
      Transaction.Input(dummyTransaction.id, 0, 1, 100, createAddress("XxQ7j37LfuXgsLd5DZAwFKhT3s2ZMkW86F")),
      Transaction.Input(dummyTransaction.id, 1, 2, 200, createAddress("XxQ7j37LfuXgsLD5DZAwFKhT3s2ZMkW86F"))
    )

    val outputs = List(
      Transaction.Output(createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"), 0, BigDecimal(50), createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"), HexString.from("00").get, None, None),
      Transaction.Output(
        createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
        1,
        BigDecimal(250),
        createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
        HexString.from("00").get,
        None, None)
    )

    val transactions = List(
      Transaction(
        createTransactionId("00051e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
        blockhash,
        12312312L,
        Size(1000),
        inputs,
        outputs),
      Transaction(
        createTransactionId("02c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
        blockhash,
        12312302L,
        Size(900),
        inputs.map(x => x.copy(value = x.value * 2)),
        outputs.map(x => x.copy(value = x.value * 2))),
      Transaction(
        createTransactionId("00c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
        blockhash,
        12312310L,
        Size(100),
        inputs.map(x => x.copy(value = x.value / 2)),
        outputs.map(x => x.copy(value = x.value / 2)))
    )

    val block = this.block.copy(
      hash = blockhash,
      height = Height(10),
      transactions = transactions.map(_.id))

    "find no results" in {
      val blockhash = createBlockhash("021d335a910f6780bdf48f9efd751b162074367eeb6740ac205223496430260f")
      val query = PaginatedQuery(Offset(0), Limit(10))
      val expected = PaginatedResult(query.offset, query.limit, Count(0), List.empty)
      val result = dataHandler.getByBlockhash(blockhash, query, defaultOrdering)

      result mustEqual Good(expected)
    }

    "find the right values" in {
      createBlock(block, transactions)

      val query = PaginatedQuery(Offset(0), Limit(10))
      val result = dataHandler.getByBlockhash(blockhash, query, defaultOrdering).get

      result.total mustEqual Count(transactions.size)
      result.offset mustEqual query.offset
      result.limit mustEqual query.limit
      result.data.size mustEqual transactions.size
    }

    def testOrdering[B](field: TransactionField)(sortBy: Transaction => B)(implicit order: Ordering[B]) = {
      createBlock(block, transactions)

      val ordering = FieldOrdering(field, OrderingCondition.AscendingOrder)
      val query = PaginatedQuery(Offset(0), Limit(10))

      val expected = transactions.sortBy(sortBy)(order).map(_.id)
      val result = dataHandler.getByBlockhash(blockhash, query, ordering).get.data
      result.map(_.id) mustEqual expected

      val expectedReverse = expected.reverse
      val resultReverse = dataHandler.getByBlockhash(blockhash, query, ordering.copy(orderingCondition = OrderingCondition.DescendingOrder)).get.data
      resultReverse.map(_.id) mustEqual expectedReverse
    }

    "allow to sort by txid" in {
      testOrdering(TransactionField.TransactionId)(_.id.string)
    }

    "allow to sort by time" in {
      testOrdering(TransactionField.Time)(_.time)
    }

    "allow to sort by sent" in {
      testOrdering(TransactionField.Sent)(_.inputs.map(_.value).sum)
    }

    "allow to sort by received" in {
      testOrdering(TransactionField.Received)(_.outputs.map(_.value).sum)
    }
  }

  "getLatestTransactionBy" should {
    "return the relation address -> latest txid" in {
      clearDatabase()
      val blocks = blockList
      blocks.map(createBlock)

      val expected = Map(
        "XcqpUChZhNkVDgQqFF9U4DdewDGUMWwG53" -> "41e315108dc2df60caddbc7e8740a5614217f996c96898019e69b3195fd7ee10",
        "XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9" -> "1e591eae200f719344fc5df0c4286e3fb191fb8a645bdf054f9b36a856fce41e"
      )

      val addresses = Every(
        createAddress("XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9"),
        createAddress("XcqpUChZhNkVDgQqFF9U4DdewDGUMWwG53"),
        createAddress("XcqpUChZhNkVDgQqFF9U4DdewDGUMWwG54"),
      )
      val result = dataHandler.getLatestTransactionBy(addresses).get
      result mustEqual expected
    }
  }

  "getBy with scroll" should {
    val address = createAddress("XxQ7j37LfuXgsLD5DZAwFKhT3s2ZMkW86F")
    val blockhash = createBlockhash("0000000000bdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
    val inputs = List(
      Transaction.Input(dummyTransaction.id, 0, 1, 100, address),
      Transaction.Input(dummyTransaction.id, 1, 2, 200, address)
    )

    val outputs = List(
      Transaction.Output(createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"), 0, BigDecimal(50), createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"), HexString.from("00").get, None, None),
      Transaction.Output(
        createTransactionId("ad9320dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"),
        1,
        BigDecimal(250),
        createAddress("Xbh5pJdBNm8J9PxnEmwVcuQKRmZZ7DkpcF"),
        HexString.from("00").get,
        None, None)
    )

    val transaction = Transaction(
      createTransactionId("00051e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
      blockhash,
      321,
      Size(1000),
      inputs,
      outputs)

    val transactions = List(
      transaction,
      transaction.copy(
        id = createTransactionId("00041e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
        time = 320),
      transaction.copy(
        id = createTransactionId("00c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
        time = 319),
      transaction.copy(
        id = createTransactionId("02c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"),
        time = 319))

    val block = this.block.copy(
      hash = blockhash,
      height = Height(10),
      transactions = transactions.map(_.id))

    def prepare() = {
      createBlock(block, transactions)
    }

    def testOrdering[B](tag: String, condition: OrderingCondition) = {
      val sorted = condition match {
        case OrderingCondition.AscendingOrder =>
          transactions
              .sortWith { case (a, b) =>
                if (a.time < b.time) true
                else if (a.time > b.time) false
                else if (a.id.string < b.id.string) true
                else false
              }

        case OrderingCondition.DescendingOrder =>
          transactions
              .sortWith { case (a, b) =>
                if (a.time > b.time) true
                else if (a.time < b.time) false
                else if (a.id.string < b.id.string) true
                else false
              }
      }

      s"[$tag] return the first elements" in {
        prepare()
        val expected = sorted.head
        val result = dataHandler.getBy(address, Limit(1), None, condition).get
        result mustEqual List(expected.copy(inputs = List.empty, outputs = List.empty))
      }

      s"[$tag] return the next elements given the last seen tx" in {
        prepare()

        val lastSeenTxid = sorted.head.id
        val expected = sorted(1)
        val result = dataHandler.getBy(address, Limit(1), Option(lastSeenTxid), condition).get
        result mustEqual List(expected.copy(inputs = List.empty, outputs = List.empty))
      }

      s"[$tag] return the element with the same time breaking ties by txid" in {
        prepare()

        val lastSeenTxid = sorted(2).id
        val expected = sorted(3)
        val result = dataHandler.getBy(address, Limit(1), Option(lastSeenTxid), condition).get
        result mustEqual List(expected.copy(inputs = List.empty, outputs = List.empty))
      }

      s"[$tag] return no elements on unknown lastSeenTransaction" in {
        val lastSeenTxid = createTransactionId("00041e4fe89466faa734d6207a7ef6115fa1dd33f7156b006ffff6bb85a79eb8")
        val result = dataHandler.getBy(address, Limit(1), Option(lastSeenTxid), condition).get
        result must be(empty)
      }
    }

    testOrdering("desc", OrderingCondition.DescendingOrder)
    testOrdering("asc", OrderingCondition.AscendingOrder)

  }

  private def createBlock(block: Block) = {
    val transactions = block.transactions
        .map(_.string)
        .map(TransactionLoader.get)
        .map(Transaction.fromRPC)

    val result = ledgerDataHandler.push(block, transactions)

    result.isGood mustEqual true
  }

  private def createBlock(block: Block, transactions: List[Transaction]) = {
    val result = ledgerDataHandler.push(block, transactions)

    result.isGood mustEqual true
  }
}
