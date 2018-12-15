package controllers

import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.alexitc.playsonify.models.ordering.FieldOrdering
import com.alexitc.playsonify.models.pagination.{Count, PaginatedQuery, PaginatedResult}
import com.alexitc.playsonify.play.PublicErrorRenderer
import com.xsn.explorer.data.TransactionBlockingDataHandler
import com.xsn.explorer.errors.{BlockNotFoundError, TransactionNotFoundError}
import com.xsn.explorer.helpers.{BlockLoader, DummyXSNService, TransactionDummyDataHandler, TransactionLoader}
import com.xsn.explorer.models._
import com.xsn.explorer.models.fields.TransactionField
import com.xsn.explorer.models.rpc.{Block, Transaction}
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{Bad, Good}
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._

import scala.concurrent.Future

class BlocksControllerSpec extends MyAPISpec {

  // PoS block
  val posBlock = BlockLoader.get("1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0")
  val posBlockCoinstakeTx = TransactionLoader.get("9165692aa53f0db6be61521daf8bece68e005396880376260872352db0c005c1")
  val posBlockCoinstakeTxInput = TransactionLoader.get("0c0f595a004eab5cf62ea70570f175701d120a0da31c8222d2d99fc60bf96577")

  // PoS block with rounding error
  val posBlockRoundingError = BlockLoader.get("25762bf01143f7fe34912c926e0b95528b082c6323de35516de0fc321f5d8058")
  val posBlockRoundingErrorCoinstakeTx = TransactionLoader.get("0b761343c7be39116d5429953e0cfbf51bfe83400ab27d61084222451045116c")
  val posBlockRoundingErrorCoinstakeTxInput = TransactionLoader.get("1860288a5a87c79e617f743af44600e050c28ddb7d929d93d43a9148e2ba6638")

  // TPoS
  val tposBlock = BlockLoader.get("19f320185015d146237efe757852b21c5e08b88b2f4de9d3fa9517d8463e472b")
  val tposBlockContractTx = TransactionLoader.get("7f2b5f25b0ae24a417633e4214827f930a69802c1c43d1fb2ff7b7075b2d1701")
  val tposBlockCoinstakeTx = TransactionLoader.get("8c7feafc18576b89bf87faf8aa89feaac1a3fad7d5da77d1fe773219a0e9d864")
  val tposBlockCoinstakeTxInput = TransactionLoader.get("9ecf10916467dccc8c8f3a87d869dc5aceb57d5d1c2117036fe60f31369a284e")

  val tposBlock2 = BlockLoader.get("a3a9fb111a3f85c3d920c2dc58ce14d541a65763834247ef958aa3b4d665ef9c")
  val tposBlock2ContractTx = TransactionLoader.get(tposBlock2.tposContract.get.string)
  val tposBlock2CoinstakeTx = TransactionLoader.get(tposBlock2.transactions(1).string)

  // PoW
  val powBlock = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
  val powBlockPreviousTx = TransactionLoader.get("67aa0bd8b9297ca6ee25a1e5c2e3a8dbbcc1e20eab76b6d1bdf9d69f8a5356b8")

  val customXSNService = new DummyXSNService {
    val blocks = Map(
      posBlock.hash -> posBlock,
      posBlockRoundingError.hash -> posBlockRoundingError,
      tposBlock.hash -> tposBlock,
      tposBlock2.hash -> tposBlock2,
      powBlock.hash -> powBlock
    )

    override def getBlock(blockhash: Blockhash): FutureApplicationResult[Block] = {
      val result = blocks.get(blockhash)
          .map(Good(_))
          .getOrElse {
            Bad(BlockNotFoundError).accumulating
          }

      Future.successful(result)
    }

    override def getRawBlock(blockhash: Blockhash): FutureApplicationResult[JsValue] = {
      val result = blocks.get(blockhash)
          .map { _ => BlockLoader.json(blockhash.string) }
          .map(Good(_))
          .getOrElse {
            Bad(BlockNotFoundError).accumulating
          }

      Future.successful(result)
    }

    override def getBlockhash(height: Height): FutureApplicationResult[Blockhash] = {
      val result = blocks
          .values
          .find(_.height == height)
          .map(_.hash)
          .map(Good(_))
          .getOrElse(Bad(BlockNotFoundError).accumulating)

      Future.successful(result)
    }

    val txs = Map(
      posBlockCoinstakeTx.id -> posBlockCoinstakeTx,
      posBlockCoinstakeTxInput.id -> posBlockCoinstakeTxInput,
      posBlockRoundingErrorCoinstakeTx.id -> posBlockRoundingErrorCoinstakeTx,
      posBlockRoundingErrorCoinstakeTxInput.id -> posBlockRoundingErrorCoinstakeTxInput,
      powBlockPreviousTx.id -> powBlockPreviousTx,
      tposBlockContractTx.id -> tposBlockContractTx,
      tposBlockCoinstakeTx.id -> tposBlockCoinstakeTx,
      tposBlockCoinstakeTxInput.id -> tposBlockCoinstakeTxInput,
      tposBlock2CoinstakeTx.id -> tposBlock2CoinstakeTx,
      tposBlock2ContractTx.id -> tposBlock2ContractTx
    )

    override def getTransaction(txid: TransactionId): FutureApplicationResult[Transaction] = {
      val result = txs.get(txid)
          .map(Good(_))
          .getOrElse {
            Bad(TransactionNotFoundError).accumulating
          }

      Future.successful(result)
    }
  }

  val transactionDataHandler = new TransactionDummyDataHandler {
    // TODO: Handle ordering
    override def getByBlockhash(blockhash: Blockhash, paginatedQuery: PaginatedQuery, ordering: FieldOrdering[TransactionField]): ApplicationResult[PaginatedResult[TransactionWithValues]] = {
      val transactions = BlockLoader
          .get(blockhash.string)
          .transactions
          .map(_.string)
          .map(TransactionLoader.get)
          .map { tx =>
            TransactionWithValues(
              id = tx.id,
              blockhash = blockhash,
              time = tx.time,
              size = tx.size,
              sent = tx.vin.flatMap(_.value).sum,
              received = tx.vout.map(_.value).sum
            )
          }

      val page = PaginatedResult(
        paginatedQuery.offset,
        paginatedQuery.limit,
        Count(transactions.size),
        transactions.drop(paginatedQuery.offset.int).take(paginatedQuery.limit.int))

      Good(page)
    }
  }

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(customXSNService))
      .overrides(bind[TransactionBlockingDataHandler].to(transactionDataHandler))
      .build()

  "GET /blocks/:query" should {
    def url(query: String) = s"/blocks/$query"

    "retrieve a PoS block" in {
      val block = posBlock
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      (jsonBlock \ "hash").as[Blockhash] mustEqual block.hash
      (jsonBlock \ "size").as[Size] mustEqual block.size
      (jsonBlock \ "bits").as[String] mustEqual block.bits
      (jsonBlock \ "chainwork").as[String] mustEqual block.chainwork
      (jsonBlock \ "difficulty").as[BigDecimal] mustEqual block.difficulty
      (jsonBlock \ "confirmations").as[Confirmations] mustEqual block.confirmations
      (jsonBlock \ "height").as[Height] mustEqual block.height
      (jsonBlock \ "medianTime").as[Long] mustEqual block.medianTime
      (jsonBlock \ "time").as[Long] mustEqual block.time
      (jsonBlock \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
      (jsonBlock \ "version").as[Long] mustEqual block.version
      (jsonBlock \ "nonce").as[Int] mustEqual block.nonce
      (jsonBlock \ "previousBlockhash").asOpt[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").asOpt[Blockhash] mustEqual block.nextBlockhash

      val jsonCoinstake = (jsonRewards \ "coinstake").as[JsValue]
      (jsonCoinstake \ "address").as[String] mustEqual "XiHW7SR56UPHeXKwcpeVsE4nUfkHv5RqE3"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal("22.49999999")

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address").as[String] mustEqual "XjUDDq221NwqRtp85wfvoDrMaaxvUCDRrY"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "retrieve a PoS block having a rounding error" in {
      val block = posBlockRoundingError
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      (jsonBlock \ "hash").as[Blockhash] mustEqual block.hash
      (jsonBlock \ "size").as[Size] mustEqual block.size
      (jsonBlock \ "bits").as[String] mustEqual block.bits
      (jsonBlock \ "chainwork").as[String] mustEqual block.chainwork
      (jsonBlock \ "difficulty").as[BigDecimal] mustEqual block.difficulty
      (jsonBlock \ "confirmations").as[Confirmations] mustEqual block.confirmations
      (jsonBlock \ "height").as[Height] mustEqual block.height
      (jsonBlock \ "medianTime").as[Long] mustEqual block.medianTime
      (jsonBlock \ "time").as[Long] mustEqual block.time
      (jsonBlock \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
      (jsonBlock \ "version").as[Long] mustEqual block.version
      (jsonBlock \ "nonce").as[Int] mustEqual block.nonce
      (jsonBlock \ "previousBlockhash").asOpt[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").asOpt[Blockhash] mustEqual block.nextBlockhash

      val jsonCoinstake = (jsonRewards \ "coinstake").as[JsValue]
      (jsonCoinstake \ "address").as[String] mustEqual "XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal("0")

      val jsonMasternode = (jsonRewards \ "masternode").asOpt[JsValue]
      jsonMasternode.isEmpty mustEqual true
    }

    "retrieve a PoW block" in {
      val block = powBlock
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      (jsonBlock \ "hash").as[Blockhash] mustEqual block.hash
      (jsonBlock \ "size").as[Size] mustEqual block.size
      (jsonBlock \ "bits").as[String] mustEqual block.bits
      (jsonBlock \ "chainwork").as[String] mustEqual block.chainwork
      (jsonBlock \ "difficulty").as[BigDecimal] mustEqual block.difficulty
      (jsonBlock \ "confirmations").as[Confirmations] mustEqual block.confirmations
      (jsonBlock \ "height").as[Height] mustEqual block.height
      (jsonBlock \ "medianTime").as[Long] mustEqual block.medianTime
      (jsonBlock \ "time").as[Long] mustEqual block.time
      (jsonBlock \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
      (jsonBlock \ "version").as[Long] mustEqual block.version
      (jsonBlock \ "nonce").as[Int] mustEqual block.nonce
      (jsonBlock \ "previousBlockhash").asOpt[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").asOpt[Blockhash] mustEqual block.nextBlockhash

      val jsonReward = (jsonRewards \ "reward").as[JsValue]
      (jsonReward \ "address").as[String] mustEqual "XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9"
      (jsonReward \ "value").as[BigDecimal] mustEqual BigDecimal("76500000")
    }

    "retrieve TPoS block" in {
      val block = tposBlock
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      (jsonBlock \ "hash").as[Blockhash] mustEqual block.hash
      (jsonBlock \ "size").as[Size] mustEqual block.size
      (jsonBlock \ "bits").as[String] mustEqual block.bits
      (jsonBlock \ "chainwork").as[String] mustEqual block.chainwork
      (jsonBlock \ "difficulty").as[BigDecimal] mustEqual block.difficulty
      (jsonBlock \ "confirmations").as[Confirmations] mustEqual block.confirmations
      (jsonBlock \ "height").as[Height] mustEqual block.height
      (jsonBlock \ "medianTime").as[Long] mustEqual block.medianTime
      (jsonBlock \ "time").as[Long] mustEqual block.time
      (jsonBlock \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
      (jsonBlock \ "version").as[Long] mustEqual block.version
      (jsonBlock \ "nonce").as[Int] mustEqual block.nonce
      (jsonBlock \ "previousBlockhash").asOpt[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").asOpt[Blockhash] mustEqual block.nextBlockhash
      (jsonBlock \ "tposContract").as[String] mustEqual block.tposContract.get.string

      val jsonOwner = (jsonRewards \ "owner").as[JsValue]
      (jsonOwner \ "address").as[String] mustEqual "Xi3sQfMQsy2CzMZTrnKW6HFGp1VqFThdLw"
      (jsonOwner \ "value").as[BigDecimal] mustEqual BigDecimal("22.275")

      val jsonMerchant = (jsonRewards \ "merchant").as[JsValue]
      (jsonMerchant \ "address").as[String] mustEqual "XyJC8xnfFrHNcMinh6gxuPRYY9HCaY9DAo"
      (jsonMerchant \ "value").as[BigDecimal] mustEqual BigDecimal("0.225")

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address").as[String] mustEqual "XydZnssXHCxxRtB4rk7evfKT9XP7GqyA9N"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "retrieve TPoS block with coinsplit" in {
      val block = tposBlock2
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      (jsonBlock \ "hash").as[Blockhash] mustEqual block.hash
      (jsonBlock \ "size").as[Size] mustEqual block.size
      (jsonBlock \ "bits").as[String] mustEqual block.bits
      (jsonBlock \ "chainwork").as[String] mustEqual block.chainwork
      (jsonBlock \ "difficulty").as[BigDecimal] mustEqual block.difficulty
      (jsonBlock \ "confirmations").as[Confirmations] mustEqual block.confirmations
      (jsonBlock \ "height").as[Height] mustEqual block.height
      (jsonBlock \ "medianTime").as[Long] mustEqual block.medianTime
      (jsonBlock \ "time").as[Long] mustEqual block.time
      (jsonBlock \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
      (jsonBlock \ "version").as[Long] mustEqual block.version
      (jsonBlock \ "nonce").as[Int] mustEqual block.nonce
      (jsonBlock \ "previousBlockhash").asOpt[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").asOpt[Blockhash] mustEqual block.nextBlockhash
      (jsonBlock \ "tposContract").as[String] mustEqual block.tposContract.get.string

      val jsonOwner = (jsonRewards \ "owner").as[JsValue]
      (jsonOwner \ "address").as[String] mustEqual "Xu5UkgRL8YRqoW6uEW8SxMLDkJwbjFVfge"
      (jsonOwner \ "value").as[BigDecimal] mustEqual BigDecimal("22.275")

      val jsonMerchant = (jsonRewards \ "merchant").as[JsValue]
      (jsonMerchant \ "address").as[String] mustEqual "XbGFpsuhv6AH3gp3dx5eQrAexP5kESh9bY"
      (jsonMerchant \ "value").as[BigDecimal] mustEqual BigDecimal("0.225")

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address").as[String] mustEqual "Xc3bKuGzy9grJZxC2ieTgQjjgyTMKSLqSM"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "retrieve a block by height" in {
      val block = posBlock
      val response = GET(url(block.height.toString))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      (jsonBlock \ "hash").as[Blockhash] mustEqual block.hash
      (jsonBlock \ "size").as[Size] mustEqual block.size
      (jsonBlock \ "bits").as[String] mustEqual block.bits
      (jsonBlock \ "chainwork").as[String] mustEqual block.chainwork
      (jsonBlock \ "difficulty").as[BigDecimal] mustEqual block.difficulty
      (jsonBlock \ "confirmations").as[Confirmations] mustEqual block.confirmations
      (jsonBlock \ "height").as[Height] mustEqual block.height
      (jsonBlock \ "medianTime").as[Long] mustEqual block.medianTime
      (jsonBlock \ "time").as[Long] mustEqual block.time
      (jsonBlock \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
      (jsonBlock \ "version").as[Long] mustEqual block.version
      (jsonBlock \ "nonce").as[Int] mustEqual block.nonce
      (jsonBlock \ "previousBlockhash").asOpt[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").asOpt[Blockhash] mustEqual block.nextBlockhash

      val jsonCoinstake = (jsonRewards \ "coinstake").as[JsValue]
      (jsonCoinstake \ "address").as[String] mustEqual "XiHW7SR56UPHeXKwcpeVsE4nUfkHv5RqE3"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal("22.49999999")

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address").as[String] mustEqual "XjUDDq221NwqRtp85wfvoDrMaaxvUCDRrY"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "fail on the wrong blockhash format" in {
      val response = GET(url("000125c06cedf38b07bff174bdb61027935dbcb34831d28cff40bedb519d5"))

      status(response) mustEqual BAD_REQUEST

      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "blockhash"
      (error \ "message").as[String].nonEmpty mustEqual true
    }

    "fail on unknown block height" in {
      val response = GET(url("-1"))

      status(response) mustEqual BAD_REQUEST

      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "blockhash"
      (error \ "message").as[String].nonEmpty mustEqual true
    }

    "fail on an unknown block" in {
      val response = GET(url("000003dc4c2fc449dededaaad6efc33ce1b64b88a060652dc47edc63d6d6b524"))

      status(response) mustEqual BAD_REQUEST

      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type").as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "blockhash"
      (error \ "message").as[String].nonEmpty mustEqual true
    }
  }

  "GET /blocks/:query/raw" should {
    def url(query: String) = s"/blocks/$query/raw"

    "retrieve a block by blockhash" in {
      val block = posBlock
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      json mustEqual BlockLoader.json(block.hash.string)
    }

    "retrieve a block by height" in {
      val block = posBlock
      val response = GET(url(block.height.toString))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      json mustEqual BlockLoader.json(block.hash.string)
    }
  }

  "GET /blocks/:blockhash/transactions" should {
    "return the transactions for the given block" in {
      val blockhash = "000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7"
      val response = GET(s"/blocks/$blockhash/transactions?offset=0&limit=5&orderBy=time:desc")

      status(response) mustEqual OK

      val json = contentAsJson(response)
      (json \ "total").as[Int] mustEqual 1
      (json \ "offset").as[Int] mustEqual 0
      (json \ "limit").as[Int] mustEqual 5

      val data = (json \ "data").as[List[JsValue]]
      data.size mustEqual 1
    }
  }
}
