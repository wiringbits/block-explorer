package com.xsn.explorer.data

import com.xsn.explorer.data.anorm.BlockPostgresDataHandler
import com.xsn.explorer.data.anorm.dao.BlockPostgresDAO
import com.xsn.explorer.data.common.PostgresDataHandlerSpec
import com.xsn.explorer.errors.BlockNotFoundError
import com.xsn.explorer.helpers.BlockLoader
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block
import org.scalactic.Bad

class BlockPostgresDataHandlerSpec extends PostgresDataHandlerSpec {

  lazy val dataHandler = new BlockPostgresDataHandler(database, new BlockPostgresDAO)

  "upsert" should {
    "add a new block" in {
      // PoS block
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")

      val result = dataHandler.upsert(block)
      result.isGood mustEqual true
      matches(block, result.get)
    }

    "override an existing block" in {
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")
      dataHandler.upsert(block)

      val newBlock = BlockLoader.get("25762bf01143f7fe34912c926e0b95528b082c6323de35516de0fc321f5d8058").copy(hash = block.hash)
      val expected = newBlock.copy(hash = block.hash)
      val result = dataHandler.upsert(newBlock)
      result.isGood mustEqual true
      matches(expected, result.get)
    }
  }

  "getBy" should {
    "return a block" in {
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")

      dataHandler.upsert(block)

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

  "delete" should {
    "delete a block" in {
      val block = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")
      dataHandler.upsert(block)

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
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")

      List(block1, block2, block0).foreach(dataHandler.upsert)

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
      val block1 = BlockLoader.get("000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7")
      val block2 = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")

      List(block1, block2, block0).map(dataHandler.upsert).foreach(_.isGood mustEqual true)

      val result = dataHandler.getFirstBlock()
      result.isGood mustEqual true
      println(result.get.height.int)
      matches(block0, result.get)
    }

    "fail on no blocks" in {
      clearDatabase()

      val result = dataHandler.getLatestBlock()
      result mustEqual Bad(BlockNotFoundError).accumulating
    }
  }

  private def clearDatabase() = {
    database.withConnection { implicit conn =>
      _root_.anorm.SQL("""DELETE FROM blocks""").execute()
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
