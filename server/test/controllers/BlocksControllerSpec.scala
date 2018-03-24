package controllers

import com.alexitc.playsonify.PublicErrorRenderer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.errors.{BlockNotFoundError, TransactionNotFoundError}
import com.xsn.explorer.helpers.{DataHelper, DummyXSNService}
import com.xsn.explorer.models._
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{Bad, Good}
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._

import scala.concurrent.Future

class BlocksControllerSpec extends MyAPISpec {

  import DataHelper._

  // PoS block
  val posBlock = createBlock(
    hash = Blockhash.from("b72dd1655408e9307ef5874be20422ee71029333283e2360975bc6073bdb2b81").get,
    transactions = List(
      TransactionId.from("7f12adbb63d443502cf151c76946d5faa0b1c662a5d67afc7da085c74e06f1ce").get,
      TransactionId.from("0834641a7d30d8a2d2b451617599670445ee94ed7736e146c13be260c576c641").get
    )
  )

  val posBlockCoinstakeTx = createTx(
    id = TransactionId.from("0834641a7d30d8a2d2b451617599670445ee94ed7736e146c13be260c576c641").get,
    vin = TransactionVIN(TransactionId.from("585cec5009c8ca19e83e33d282a6a8de65eb2ca007b54d6572167703768967d9").get, 2),
    vout = List(
      TransactionVOUT(n = 0, value = BigDecimal("0")),
      createTransactionVOUT(1, BigDecimal(600), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"))),
      createTransactionVOUT(2, BigDecimal(600), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"))),
      createTransactionVOUT(3, BigDecimal(10), createScriptPubKey("pubkeyhash", createAddress("XnH3bC9NruJ4wnu4Dgi8F3wemmJtcxpKp6")))
    )
  )

  val posBlockCoinstakeTxInput = createTx(
    id = TransactionId.from("585cec5009c8ca19e83e33d282a6a8de65eb2ca007b54d6572167703768967d9").get,
    vin = TransactionVIN(TransactionId.from("fd74206866fc4ed986d39084eb9f20de6cb324b028693f33d60897ac995fff4f").get, 2),
    vout = List(
      TransactionVOUT(BigDecimal("0"), 0, None),
      createTransactionVOUT(1, BigDecimal(1), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"))),
      createTransactionVOUT(2, BigDecimal(1000), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL")))
    )
  )

  // PoS block with rounding error
  val posBlockRoundingError = createBlock(
    hash = Blockhash.from("25762bf01143f7fe34912c926e0b95528b082c6323de35516de0fc321f5d8058").get,
    transactions = List(
      TransactionId.from("df275d713fcf5e78e7e8369d640201d46736c0d2255e31ce45bd5aa0206f861f").get,
      TransactionId.from("0b761343c7be39116d5429953e0cfbf51bfe83400ab27d61084222451045116c").get
    )
  )

  val posBlockRoundingErrorCoinstakeTx = createTx(
    id = TransactionId.from("0b761343c7be39116d5429953e0cfbf51bfe83400ab27d61084222451045116c").get,
    vin = TransactionVIN(TransactionId.from("1860288a5a87c79e617f743af44600e050c28ddb7d929d93d43a9148e2ba6638").get, 1),
    vout = List(
      TransactionVOUT(BigDecimal("0"), 0, None),
      createTransactionVOUT(1, BigDecimal("292968.74570312"), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"))),
      createTransactionVOUT(2, BigDecimal("292968.74570312"), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL")))
    )
  )

  val posBlockRoundingErrorCoinstakeTxInput = createTx(
    id = TransactionId.from("1860288a5a87c79e617f743af44600e050c28ddb7d929d93d43a9148e2ba6638").get,
    vin = TransactionVIN(TransactionId.from("ef157f5ec0b3a6cdf669ff799988ee94d9fa2af8adaf2408ae9e34b47310831f").get, 2),
    vout = List(
      TransactionVOUT(BigDecimal("0"), 0, None),
      createTransactionVOUT(1, BigDecimal("585937.49140625"), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"))),
      createTransactionVOUT(2, BigDecimal("585937.49140625"), createScriptPubKey("pubkeyhash", createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL")))
    )
  )

  // TPoS
  val tposBlock = posBlock.copy(
    hash = Blockhash.from("19f320185015d146237efe757852b21c5e08b88b2f4de9d3fa9517d8463e472b").get,
    tposContract = Some(createTransactionId("7f2b5f25b0ae24a417633e4214827f930a69802c1c43d1fb2ff7b7075b2d1701")),
    transactions = List(
      createTransactionId("28568eb4a2c69a292b7d56daa45e3b17fbfc8af9310d5c2d444600e64266c87f"),
      createTransactionId("8c7feafc18576b89bf87faf8aa89feaac1a3fad7d5da77d1fe773219a0e9d864")
    )
  )

  val tposBlockContractTx = createTx(
    createTransactionId("7f2b5f25b0ae24a417633e4214827f930a69802c1c43d1fb2ff7b7075b2d1701"),
    None,
    List(
      TransactionVOUT(
        n = 0,
        value = BigDecimal(0),
        scriptPubKey = Some(createScriptPubKey("nulldata", "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99"))
      )
    )
  )

  val tposBlockCoinstakeTx = createTx(
    createTransactionId("8c7feafc18576b89bf87faf8aa89feaac1a3fad7d5da77d1fe773219a0e9d864"),
    Some(TransactionVIN(createTransactionId("9ecf10916467dccc8c8f3a87d869dc5aceb57d5d1c2117036fe60f31369a284e"), 1)),
    List(
      TransactionVOUT(BigDecimal(0), 0, None),
      TransactionVOUT(
        n = 1,
        value = BigDecimal("1022.27500000"),
        scriptPubKey = Some(createScriptPubKey("pubkeyhash", createAddress("Xi3sQfMQsy2CzMZTrnKW6HFGp1VqFThdLw")))
      ),

      TransactionVOUT(
        n = 2,
        value = BigDecimal("0.22500000"),
        scriptPubKey = Some(createScriptPubKey("pubkeyhash", createAddress("XyJC8xnfFrHNcMinh6gxuPRYY9HCaY9DAo")))
      ),

      TransactionVOUT(
        n = 3,
        value = BigDecimal("22.50000000"),
        scriptPubKey = Some(createScriptPubKey("pubkeyhash", createAddress("XydZnssXHCxxRtB4rk7evfKT9XP7GqyA9N")))
      )
    )
  )

  val tposBlockCoinstakeTxInput = createTx(
    createTransactionId("9ecf10916467dccc8c8f3a87d869dc5aceb57d5d1c2117036fe60f31369a284e"),
    None,
    List(
      TransactionVOUT(n = 1, value = BigDecimal(1000))
    )
  )

  // PoW
  val powBlock = posBlock.copy(
    hash = Blockhash.from("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8").get,
    transactions = List(
      TransactionId.from("67aa0bd8b9297ca6ee25a1e5c2e3a8dbbcc1e20eab76b6d1bdf9d69f8a5356b8").get
    ),
    height = Height(2)
  )

  val powBlockPreviousTx = createTx(
    id = TransactionId.from("67aa0bd8b9297ca6ee25a1e5c2e3a8dbbcc1e20eab76b6d1bdf9d69f8a5356b8").get,
    vin = None,
    vout = List(
      createTransactionVOUT(0, BigDecimal("76500000.00000000"), createScriptPubKey("pubkey", createAddress("XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9")))
    )
  )

  val customXSNService = new DummyXSNService {
    val blocks = Map(
      posBlock.hash -> posBlock,
      posBlockRoundingError.hash -> posBlockRoundingError,
      tposBlock.hash -> tposBlock,
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

    val txs = Map(
      posBlockCoinstakeTx.id -> posBlockCoinstakeTx,
      posBlockCoinstakeTxInput.id -> posBlockCoinstakeTxInput,
      posBlockRoundingErrorCoinstakeTx.id -> posBlockRoundingErrorCoinstakeTx,
      posBlockRoundingErrorCoinstakeTxInput.id -> posBlockRoundingErrorCoinstakeTxInput,
      powBlockPreviousTx.id -> powBlockPreviousTx,
      tposBlockContractTx.id -> tposBlockContractTx,
      tposBlockCoinstakeTx.id -> tposBlockCoinstakeTx,
      tposBlockCoinstakeTxInput.id -> tposBlockCoinstakeTxInput
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

  override val application = guiceApplicationBuilder
      .overrides(bind[XSNService].to(customXSNService))
      .build()

  "GET /blocks/:blockhash" should {
    def url(blockhash: String) = s"/blocks/$blockhash"

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
      (jsonCoinstake \ "address").as[String] mustEqual "XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal("200")

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address").as[String] mustEqual "XnH3bC9NruJ4wnu4Dgi8F3wemmJtcxpKp6"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("10")
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
      val response = GET(url("19f320185015d146237efe757852b21c5e08b88b2f4de9d3fa9517d8463e472b"))

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

  private def createTx(id: TransactionId, vin: TransactionVIN, vout: List[TransactionVOUT]): Transaction = {
    createTx(id, Some(vin), vout)
  }

  private def createTx(id: TransactionId, vin: Option[TransactionVIN], vout: List[TransactionVOUT]): Transaction = {
    Transaction(
      id = id,
      size = Size(234),
      blockhash = Blockhash.from("b72dd1655408e9307ef5874be20422ee71029333283e2360975bc6073bdb2b81").get,
      time = 1520318120,
      blocktime = 1520318120,
      confirmations = Confirmations(1950),
      vin = vin,
      vout = vout
    )
  }

  private def createBlock(hash: Blockhash, transactions: List[TransactionId]): Block = {
    Block(
      hash = hash,
      transactions = transactions,
      confirmations = Confirmations(11189),
      size = Size(478),
      height = Height(809),
      version = 536870912,
      merkleRoot = Blockhash.from("598cc6ba8238d87641b0dbd02485b7d635b5417429df3145c98c3ff8779ab4b8").get,
      time = 1520318054,
      medianTime = 1520318054,
      nonce = 0,
      bits = "1d011212",
      chainwork = "00000000000000000000000000000000000000000000000000000084c71ff420",
      difficulty = BigDecimal("0.9340526210769362"),
      previousBlockhash = Some(Blockhash.from("000003dc4c2fc449dededaaad6efc33ce1b64b88a060652dc47edc63d6d6b524").get),
      nextBlockhash = Some(Blockhash.from("000000125c06cedf38b07bff174bdb61027935dbcb34831d28cff40bedb519d5").get),
      tposContract = None
    )
  }
}
