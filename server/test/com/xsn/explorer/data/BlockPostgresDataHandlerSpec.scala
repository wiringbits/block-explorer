package com.xsn.explorer.data

import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition}
import com.alexitc.playsonify.models.pagination.{Count, Limit, Offset, PaginatedQuery}
import com.xsn.explorer.data.anorm.BlockPostgresDataHandler
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.helpers.{BlockLoader, DataHandlerObjects}
import com.xsn.explorer.models.fields.BlockField
import com.xsn.explorer.models.persisted.Block
import com.xsn.explorer.models.values.Blockhash
import org.scalactic.{Bad, One, Or}
import org.scalatest.BeforeAndAfter

class BlockPostgresDataHandlerSpec extends PostgresDataHandlerSpec with BeforeAndAfter {

  import DataHandlerObjects._

  before {
    clearDatabase()
  }

  val dao = blockPostgresDAO
  val defaultOrdering = FieldOrdering(BlockField.Height, OrderingCondition.AscendingOrder)
  lazy val dataHandler = new BlockPostgresDataHandler(database, dao)

  def insert(block: Block) = {
    database.withConnection { implicit conn =>
      val maybe = dao.insert(block)
      Or.from(maybe, One(BlockNotFoundError))
    }
  }

  "getBy blockhash" should {
    "return a block" in {
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")
          .copy(previousBlockhash = None, nextBlockhash = None)

      insert(block).isGood mustEqual true

      val result = dataHandler.getBy(block.hash)
      result.isGood mustEqual true
      matches(block, result.get)
    }

    "fail on block not found" in {
      val blockhash = Blockhash.from("b858d38a3552c83aea58f66fe00ae220352a235e33fcf1f3af04507a61a9dc32").get

      val result = dataHandler.getBy(blockhash)
      result mustEqual Bad(BlockNotFoundError).accumulating
    }
  }

  "getBy height" should {
    "return a block" in {
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")
          .copy(previousBlockhash = None, nextBlockhash = None)

      insert(block).isGood mustEqual true

      val result = dataHandler.getBy(block.height)
      result.isGood mustEqual true
      matches(block, result.get)
    }

    "fail on block not found" in {
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")

      val result = dataHandler.getBy(block.height)
      result mustEqual Bad(BlockNotFoundError).accumulating
    }
  }

  "getBy" should {
    "paginate the results" in {
      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
          .copy(previousBlockhash = None, nextBlockhash = None)
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
          .copy(nextBlockhash = None)
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
          .copy(nextBlockhash = None)

      List(block0, block1, block2).map(insert).foreach(_.isGood mustEqual true)

      val query = PaginatedQuery(Offset(1), Limit(3))
      val expected = List(block1, block2)
      val result = dataHandler.getBy(query, defaultOrdering).get

      result.total mustEqual Count(3)
      result.offset mustEqual query.offset
      result.limit mustEqual query.limit
      result.data.size mustEqual expected.size

      val data = result.data
      matches(data(0), expected(0))
      matches(data(1), expected(1))
    }


    def testOrdering[B](field: BlockField)(sortBy: Block => B)(implicit order: Ordering[B]) = {
      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
          .copy(previousBlockhash = None, nextBlockhash = None)
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
          .copy(nextBlockhash = None)
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
          .copy(nextBlockhash = None)

      val blocks = List(block0, block1, block2)
      blocks.map(insert).foreach(_.isGood mustEqual true)


      val ordering = FieldOrdering(field, OrderingCondition.AscendingOrder)
      val query = PaginatedQuery(Offset(0), Limit(10))

      val expected = blocks.sortBy(sortBy)(order).map(_.hash)
      val result = dataHandler.getBy(query, ordering).get.data
      result.map(_.hash) mustEqual expected

      val expectedReverse = expected.reverse
      val resultReverse = dataHandler.getBy(query, ordering.copy(orderingCondition = OrderingCondition.DescendingOrder)).get.data
      resultReverse.map(_.hash) mustEqual expectedReverse
    }

    "allow to sort by txid" in {
      testOrdering(BlockField.Height)(_.height.int)
    }

    "allow to sort by time" in {
      testOrdering(BlockField.Time)(_.time)
    }
  }

  "delete" should {
    "delete a block" in {
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")
          .copy(previousBlockhash = None, nextBlockhash = None)
      insert(block).isGood mustEqual true

      val result = dataHandler.delete(block.hash)
      result.isGood mustEqual true
      matches(block, result.get)
    }

    "fail on block not found" in {
      val blockhash = Blockhash.from("b858d38a3552c83aea58f66fe00ae220352a235e33fcf1f3af04507a61a9dc32").get

      val result = dataHandler.delete(blockhash)
      result mustEqual Bad(BlockNotFoundError).accumulating
    }
  }

  "getLatestBlock" should {
    "return the block" in {
      clearDatabase()

      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
          .copy(previousBlockhash = None, nextBlockhash = None)
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
          .copy(nextBlockhash = None)
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
          .copy(nextBlockhash = None)

      List(block0, block1, block2).map(insert).foreach(_.isGood mustEqual true)

      val result = dataHandler.getLatestBlock()
      result.isGood mustEqual true
      matches(block2, result.get)
    }

    "fail on no blocks" in {
      clearDatabase()

      val result = dataHandler.getLatestBlock()
      result mustEqual Bad(BlockNotFoundError).accumulating
    }
  }

  "getFirstBlock" should {
    "return the block" in {
      clearDatabase()

      val block0 = BlockLoader.get("00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34")
          .copy(previousBlockhash = None, nextBlockhash = None)
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
          .copy(nextBlockhash = None)
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
          .copy(nextBlockhash = None)

      List(block0, block1, block2).map(insert).foreach(_.isGood mustEqual true)

      val result = dataHandler.getFirstBlock()
      result.isGood mustEqual true
      matches(block0, result.get)
    }

    "fail on no blocks" in {
      clearDatabase()

      val result = dataHandler.getLatestBlock()
      result mustEqual Bad(BlockNotFoundError).accumulating
    }
  }

  private def matches(expected: Block, result: Block) = {
    // NOTE: transactions and confirmations are not matched intentionally
    result.hash mustEqual expected.hash
    result.tposContract mustEqual expected.tposContract
    result.nextBlockhash mustEqual expected.nextBlockhash
    result.previousBlockhash mustEqual expected.previousBlockhash
    result.merkleRoot mustEqual expected.merkleRoot
    result.size mustEqual expected.size
    result.height mustEqual expected.height
    result.version mustEqual expected.version
    result.medianTime mustEqual expected.medianTime
    result.time mustEqual expected.time
    result.bits mustEqual expected.bits
    result.chainwork mustEqual expected.chainwork
    result.difficulty mustEqual expected.difficulty
    result.nonce mustEqual expected.nonce
  }
}
