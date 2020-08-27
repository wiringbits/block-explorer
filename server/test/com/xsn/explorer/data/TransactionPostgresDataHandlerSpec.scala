package com.xsn.explorer.data

import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination._
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.{BlockNotFoundError, TransactionError}
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers.Converters._
import com.xsn.explorer.helpers.DataHandlerObjects._
import com.xsn.explorer.helpers.DataHelper._
import com.xsn.explorer.helpers.{DataGenerator, LedgerHelper, TransactionLoader}
import com.xsn.explorer.models.{BlockReward, PoWBlockRewards, TransactionInfo}
import com.xsn.explorer.models.fields.TransactionField
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.values._
import org.scalactic.{Bad, Good, One, Or}
import org.scalatest.BeforeAndAfter
import org.scalatest.EitherValues._

class TransactionPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  import DataGenerator._

  private val emptyFilterFactory = () => GolombCodedSet(1, 2, 3, List(new UnsignedByte(0.toByte)))

  lazy val dataHandler = createTransactionDataHandler(database)
  lazy val ledgerDataHandler = createLedgerDataHandler(database)
  lazy val blockDataHandler = createBlockDataHandler(database)

  val defaultOrdering = FieldOrdering(TransactionField.Time, OrderingCondition.DescendingOrder)

  val blockList = LedgerHelper.blockList.take(6)

  val block = DataGenerator.randomBlock()

  val dummyTransaction = {
    val tx = randomTransaction(blockhash = block.hash, utxos = List.empty)
    val outputs = DataGenerator.randomOutputs(3).map(_.copy(txid = tx.id))
    tx.copy(outputs = outputs)
  }

  before {
    clearDatabase()
    prepareBlock(block)
    prepareTransaction(dummyTransaction)
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
        script = HexString.from("2103e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029ac").get
      )

      val result = dataHandler.getUnspentOutputs(expected.address.get).get

      result mustEqual List(expected)
    }
  }

  "getOutput" should {
    val txid = createTransactionId("67aa0bd8b9297ca6ee25a1e5c2e3a8dbbcc1e20eab76b6d1bdf9d69f8a5356b8")

    "return the output" in {
      val index = 0
      val expected = Transaction.Output(
        address = createAddress("XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9"),
        txid = createTransactionId("67aa0bd8b9297ca6ee25a1e5c2e3a8dbbcc1e20eab76b6d1bdf9d69f8a5356b8"),
        index = 0,
        value = BigDecimal(76500000.000000000000000),
        script = HexString.from("2103e8c52f2c5155771492907095753a43ce776e1fa7c5e769a67a9f3db4467ec029ac").get
      )

      clearDatabase()
      val blocks = blockList.take(3)
      blocks.map(createBlock)

      val result = dataHandler.getOutput(txid, index)
      result must be(Good(expected))
    }

    "return no value" in {
      val index = 10

      val result = dataHandler.getOutput(txid, index)
      result must be(Bad(TransactionError.OutputNotFound(txid, index)).accumulating)
    }

    "not corrupt the output" in {
      val txid = createTransactionId("9969603dca74d14d29d1d5f56b94c7872551607f8c2d6837ab9715c60721b50e")
      val rpcTx = TransactionLoader.getWithValues(txid.string).copy(blockhash = block.hash)
      val tx = Transaction.fromRPC(rpcTx)._1
      val expected = Transaction.Output(
        txid = txid,
        index = 2,
        value = 0.01,
        addresses = List.empty,
        script = HexString
          .from("0804678afd04678afd75a820894eeb82f9a851f5d1cb1be3249f58bc8d259963832c5e7474a76f7a859ee95c87")
          .get
      )

      upsertTransaction(tx)
      val result = dataHandler.getOutput(txid, 2)
      result.toEither.right.value must be(expected)
    }
  }

  "getBy with scroll" should {
    val address = randomAddress
    val blockhash = randomBlockhash
    val inputs = List(
      Transaction.Input(dummyTransaction.id, 0, 1, 100, address),
      Transaction.Input(dummyTransaction.id, 1, 2, 200, address)
    )

    val outputs = List(
      Transaction.Output(randomTransactionId, 0, BigDecimal(50), randomAddress, randomHexString()),
      Transaction.Output(randomTransactionId, 1, BigDecimal(250), randomAddress, HexString.from("00").get)
    )

    val transactions = List.fill(4)(randomTransactionId).zip(List(321L, 320L, 319L, 319L)).map {
      case (txid, time) =>
        Transaction.HasIO(Transaction(txid, blockhash, time, Size(1000)), inputs, outputs.map(_.copy(txid = txid)))
    }

    val block = randomBlock(blockhash = blockhash).copy(transactions = transactions.map(_.id))

    def prepare() = {
      createBlock(block, transactions)
    }

    def testOrdering[B](tag: String, condition: OrderingCondition) = {
      val sorted = condition match {
        case OrderingCondition.AscendingOrder =>
          transactions
            .sortWith {
              case (a, b) =>
                if (a.time < b.time) true
                else if (a.time > b.time) false
                else a.id.string.compareTo(b.id.string) < 0
            }

        case OrderingCondition.DescendingOrder =>
          transactions
            .sortWith {
              case (a, b) =>
                if (a.time > b.time) true
                else if (a.time < b.time) false
                else a.id.string.compareTo(b.id.string) < 0
            }
      }

      def matchOnlyData(expected: Transaction.HasIO, actual: Transaction.HasIO) = {
        actual.copy(inputs = List.empty, outputs = List.empty) mustEqual expected.copy(
          inputs = List.empty,
          outputs = List.empty
        )
      }

      s"[$tag] return the first elements" in {
        prepare()
        val expected = sorted.head
        val result = dataHandler.getBy(address, Limit(1), None, condition).get

        matchOnlyData(expected, result.head)
      }

      s"[$tag] return the next elements given the last seen tx" in {
        prepare()

        val lastSeenTxid = sorted.head.id
        val expected = sorted(1)
        val result = dataHandler.getBy(address, Limit(1), Option(lastSeenTxid), condition).get
        matchOnlyData(expected, result.head)
      }

      s"[$tag] return the element with the same time breaking ties by txid" in {
        prepare()

        val lastSeenTxid = sorted(2).id
        val expected = sorted(3)
        val result = dataHandler.getBy(address, Limit(1), Option(lastSeenTxid), condition).get
        matchOnlyData(expected, result.head)
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

  "get with scroll" should {
    val address = randomAddress
    val blockhash = randomBlockhash
    val inputs = List(
      Transaction.Input(dummyTransaction.id, 0, 1, 100, address),
      Transaction.Input(dummyTransaction.id, 1, 2, 200, address)
    )

    val outputs = List(
      Transaction.Output(randomTransactionId, 0, BigDecimal(50), randomAddress, randomHexString()),
      Transaction.Output(randomTransactionId, 1, BigDecimal(250), randomAddress, HexString.from("00").get)
    )

    val transactions = List.fill(4)(randomTransactionId).zip(List(321L, 320L, 319L, 319L)).map {
      case (txid, time) =>
        Transaction.HasIO(Transaction(txid, blockhash, time, Size(1000)), inputs, outputs.map(_.copy(txid = txid)))
    }

    val block = randomBlock(blockhash = blockhash).copy(transactions = transactions.map(_.id))

    def prepare() = {
      createBlock(block, transactions)
    }

    val sorted = transactions
      .sortWith {
        case (a, b) =>
          if (a.time > b.time) true
          else if (a.time < b.time) false
          else a.id.string.compareTo(b.id.string) < 0
      }

    "return the last element without last seen tx" in {
      prepare()
      val expected = TransactionInfo(
        dummyTransaction.id, dummyTransaction.blockhash, dummyTransaction.time, dummyTransaction.size, BigDecimal(300), BigDecimal(300), Height(0)
      )
      val result = dataHandler.get(Limit(1), None, OrderingCondition.DescendingOrder).get

      result.head.id mustEqual expected.id
      result.head.blockhash mustEqual expected.blockhash
    }

    "return the next elements given the last seen tx" in {
      prepare()

      val lastSeenTxid = dummyTransaction.id
      
      val txWithIO = sorted.head
      val expected = TransactionInfo(
        txWithIO.id, txWithIO.blockhash, txWithIO.time, txWithIO.size, BigDecimal(0), BigDecimal(0), Height(0)
      )

      val result = dataHandler.get(Limit(1), Option(lastSeenTxid), OrderingCondition.DescendingOrder).get

      result.head.id mustEqual expected.id
      result.head.blockhash mustEqual expected.blockhash
    }

    "return no elements on unknown lastSeenTransaction" in {
      val lastSeenTxid = createTransactionId("00041e4fe89466faa734d6207a7ef6115fa1dd33f7156b006ffff6bb85a79eb8")
      val result = dataHandler.get(Limit(1), Option(lastSeenTxid), OrderingCondition.DescendingOrder).get

      result must be(empty)
    }
  }

  "spending an output" should {
    "use the right values" in {
      val address = randomAddress
      val blockhash = randomBlockhash
      val inputs = List(
        Transaction.Input(dummyTransaction.id, 0, 1, 100, address),
        Transaction.Input(dummyTransaction.id, 1, 2, 200, address)
      )

      val newTxid = randomTransactionId
      val outputs = List(
        Transaction.Output(newTxid, 0, BigDecimal(50), randomAddress, randomHexString()),
        Transaction.Output(newTxid, 1, BigDecimal(250), randomAddress, randomHexString())
      )

      val transaction = Transaction.HasIO(Transaction(newTxid, blockhash, 321, Size(1000)), inputs, outputs)

      val newTxid2 = randomTransactionId
      val newAddress = randomAddress
      val transaction2 = transaction.copy(
        transaction = transaction.transaction.copy(id = newTxid2),
        inputs = List(
          Transaction.Input(
            fromTxid = transaction.id,
            fromOutputIndex = 0,
            index = 0,
            value = transaction.outputs(0).value,
            address = newAddress
          ),
          Transaction.Input(
            fromTxid = transaction.id,
            fromOutputIndex = 1,
            index = 1,
            value = transaction.outputs(1).value,
            address = newAddress
          )
        ),
        outputs = transaction.outputs.map(_.copy(txid = newTxid2))
      )

      val transactions = List(transaction, transaction2)

      val block = this.block.copy(hash = blockhash, height = Height(10), transactions = transactions.map(_.id))

      createBlock(block, transactions)

      // check that the outputs are properly spent
      database.withConnection { implicit conn =>
        import _root_.anorm._

        val spentOn = SQL(
          s"""
            |SELECT ENCODE(spent_on, 'hex') as spent_on_str
            |FROM transaction_outputs
            |WHERE txid = DECODE('${transaction.id.string}', 'hex')
          """.stripMargin
        ).as(SqlParser.str("spent_on_str").*)

        spentOn.foreach(_ mustEqual transaction2.id.string)
      }

      // check that the inputs are linked to the correct output
      database.withConnection { implicit conn =>
        import _root_.anorm._

        val query = SQL(
          s"""
             |SELECT ENCODE(from_txid, 'hex') AS from_txid_str, from_output_index
             |FROM transaction_inputs
             |WHERE txid = DECODE('${transaction2.id}', 'hex')
          """.stripMargin
        )

        val fromTxid = query.as(SqlParser.str("from_txid_str").*)
        fromTxid.foreach(_ mustEqual transaction.id.string)

        val fromOutputIndex = query.as(SqlParser.int("from_output_index").*)
        fromOutputIndex.sorted mustEqual List(0, 1)
      }
    }
  }

  private def createBlock(block: Block.Canonical) = {
    val transactions = block.transactions
      .map(_.string)
      .map(TransactionLoader.getWithValues(_))
      .map(Transaction.fromRPC)
      .map(_._1)

    val reward = PoWBlockRewards(BlockReward(DataGenerator.randomAddress, 100))
    val result =
      ledgerDataHandler.push(block.withTransactions(transactions), List.empty, emptyFilterFactory, Some(reward))

    result.isGood mustEqual true
  }

  private def createBlock(block: Block.Canonical, transactions: List[Transaction.HasIO]) = {
    val reward = PoWBlockRewards(BlockReward(DataGenerator.randomAddress, 100))
    val result =
      ledgerDataHandler.push(block.withTransactions(transactions), List.empty, emptyFilterFactory, Some(reward))

    result.isGood mustEqual true
  }

  private def prepareBlock(block: Block.Canonical) = {
    try {
      database.withConnection { implicit conn =>
        val maybe = blockPostgresDAO.insert(block)
        Or.from(maybe, One(BlockNotFoundError))
      }
    } catch {
      case _: Throwable => ()
    }
  }

  private def prepareTransaction(transaction: Transaction.HasIO) = {
    try {
      upsertTransaction(transaction)
    } catch {
      case _: Throwable => ()
    }
  }

  private def upsertTransaction(transaction: Transaction.HasIO) = {
    database.withConnection { implicit conn =>
      val maybe = transactionPostgresDAO.upsert(1, transaction)
      Or.from(maybe, One(TransactionError.NotFound(transaction.id)))
    }.isGood must be(true)
  }
}
