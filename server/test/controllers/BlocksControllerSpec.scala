package controllers

import com.alexitc.playsonify.PublicErrorRenderer
import com.alexitc.playsonify.core.FutureApplicationResult
import com.xsn.explorer.errors.{BlockNotFoundError, TransactionNotFoundError}
import com.xsn.explorer.helpers.DummyXSNService
import com.xsn.explorer.models._
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.scalactic.{Bad, Good}
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._

import scala.concurrent.Future

class BlocksControllerSpec extends MyAPISpec {

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
      TransactionVOUT(BigDecimal("0"), 0, "nonstandard", None),
      TransactionVOUT(n = 1, value = BigDecimal("600"), scriptPubKeyType = "pubkeyhash", address = Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL")),
      TransactionVOUT(n = 2, value = BigDecimal("600"), scriptPubKeyType = "pubkeyhash", address = Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL")),
      TransactionVOUT(BigDecimal("10"), 3, "pubkeyhash", Some(Address.from("XnH3bC9NruJ4wnu4Dgi8F3wemmJtcxpKp6").get))
    )
  )

  val posBlockCoinstakeTxInput = createTx(
    id = TransactionId.from("585cec5009c8ca19e83e33d282a6a8de65eb2ca007b54d6572167703768967d9").get,
    vin = TransactionVIN(TransactionId.from("fd74206866fc4ed986d39084eb9f20de6cb324b028693f33d60897ac995fff4f").get, 2),
    vout = List(
      TransactionVOUT(BigDecimal("0"), 0, "nonstandard", None),
      TransactionVOUT(BigDecimal("1"), 1, "pubkeyhash", Some(Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL").get)),
      TransactionVOUT(BigDecimal("1000"), 2, "pubkeyhash", Some(Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL").get))
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
      TransactionVOUT(BigDecimal("0"), 0, "nonstandard", None),
      TransactionVOUT(n = 1, value = BigDecimal("292968.74570312"), scriptPubKeyType = "pubkeyhash", address = Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL")),
      TransactionVOUT(n = 2, value = BigDecimal("292968.74570312"), scriptPubKeyType = "pubkeyhash", address = Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"))
    )
  )

  val posBlockRoundingErrorCoinstakeTxInput = createTx(
    id = TransactionId.from("1860288a5a87c79e617f743af44600e050c28ddb7d929d93d43a9148e2ba6638").get,
    vin = TransactionVIN(TransactionId.from("ef157f5ec0b3a6cdf669ff799988ee94d9fa2af8adaf2408ae9e34b47310831f").get, 2),
    vout = List(
      TransactionVOUT(BigDecimal("0"), 0, "nonstandard", None),
      TransactionVOUT(BigDecimal("585937.49140625"), 1, "pubkeyhash", Some(Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL").get)),
      TransactionVOUT(BigDecimal("585937.49140625"), 2, "pubkeyhash", Some(Address.from("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL").get))
    )
  )

  val customXSNService = new DummyXSNService {
    val blocks = Map(
      posBlock.hash -> posBlock,
      posBlockRoundingError.hash -> posBlockRoundingError
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
      posBlockRoundingErrorCoinstakeTxInput.id -> posBlockRoundingErrorCoinstakeTxInput
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
      (jsonBlock \ "previousBlockhash").as[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").as[Blockhash] mustEqual block.nextBlockhash

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
      (jsonBlock \ "previousBlockhash").as[Blockhash] mustEqual block.previousBlockhash
      (jsonBlock \ "nextBlockhash").as[Blockhash] mustEqual block.nextBlockhash

      val jsonCoinstake = (jsonRewards \ "coinstake").as[JsValue]
      (jsonCoinstake \ "address").as[String] mustEqual "XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal("0")

      val jsonMasternode = (jsonRewards \ "masternode").asOpt[JsValue]
      jsonMasternode.isEmpty mustEqual true
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
    Transaction(
      id = id,
      size = Size(234),
      blockhash = Blockhash.from("b72dd1655408e9307ef5874be20422ee71029333283e2360975bc6073bdb2b81").get,
      time = 1520318120,
      blocktime = 1520318120,
      confirmations = Confirmations(1950),
      vin = Some(vin),
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
      previousBlockhash = Blockhash.from("000003dc4c2fc449dededaaad6efc33ce1b64b88a060652dc47edc63d6d6b524").get,
      nextBlockhash = Blockhash.from("000000125c06cedf38b07bff174bdb61027935dbcb34831d28cff40bedb519d5").get
    )
  }
}
