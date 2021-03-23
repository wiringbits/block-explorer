package controllers

import com.alexitc.playsonify.models.pagination._
import com.alexitc.playsonify.play.PublicErrorRenderer
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.xsn.explorer.data.{BlockBlockingDataHandler, TransactionBlockingDataHandler}
import com.xsn.explorer.gcs.{GolombCodedSet, UnsignedByte}
import com.xsn.explorer.helpers._
import com.xsn.explorer.models.persisted.{BlockHeader, BlockInfo, Transaction}
import com.xsn.explorer.models.rpc.Block
import com.xsn.explorer.models.values.{Blockhash, Confirmations, Height, Size, TransactionId}
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import io.scalaland.chimney.dsl._
import org.mockito.ArgumentMatchersSugar._
import org.mockito.MockitoSugar._
import org.scalactic.Good
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import play.api.libs.json.Json

class BlocksControllerSpec extends MyAPISpec {

  // PoS block
  private val posBlock = BlockLoader.getRPC(
    "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0"
  )

  private val customXSNService = new FileBasedXSNService

  private val transactionDataHandlerMock = mock[TransactionBlockingDataHandler]
  private val blockBlockingDataHandlerMock = mock[BlockBlockingDataHandler]

  override val application = guiceApplicationBuilder
    .overrides(bind[XSNService].to(customXSNService))
    .overrides(
      bind[TransactionBlockingDataHandler].to(transactionDataHandlerMock)
    )
    .overrides(bind[BlockBlockingDataHandler].to(blockBlockingDataHandlerMock))
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

      matchBlock(block, jsonBlock)

      val jsonCoinstake = (jsonRewards \ "coinstake").as[JsValue]
      (jsonCoinstake \ "address")
        .as[String] mustEqual "XiHW7SR56UPHeXKwcpeVsE4nUfkHv5RqE3"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal(
        "22.49999999"
      )

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address")
        .as[String] mustEqual "XjUDDq221NwqRtp85wfvoDrMaaxvUCDRrY"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "retrieve a PoS block having a rounding error" in {
      val posBlockRoundingError = BlockLoader.getRPC(
        "25762bf01143f7fe34912c926e0b95528b082c6323de35516de0fc321f5d8058"
      )
      val block = posBlockRoundingError
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      matchBlock(block, jsonBlock)

      val jsonCoinstake = (jsonRewards \ "coinstake").as[JsValue]
      (jsonCoinstake \ "address")
        .as[String] mustEqual "XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal("0")

      val jsonMasternode = (jsonRewards \ "masternode").asOpt[JsValue]
      jsonMasternode.isEmpty mustEqual true
    }

    "retrieve a PoW block" in {
      val powBlock = BlockLoader.getRPC(
        "000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8"
      )
      val block = powBlock
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      matchBlock(block, jsonBlock)

      val jsonReward = (jsonRewards \ "reward").as[JsValue]
      (jsonReward \ "address")
        .as[String] mustEqual "XdJnCKYNwzCz8ATv8Eu75gonaHyfr9qXg9"
      (jsonReward \ "value").as[BigDecimal] mustEqual BigDecimal("76500000")
    }

    "retrieve TPoS block" in {
      val tposBlock = BlockLoader.getRPC(
        "19f320185015d146237efe757852b21c5e08b88b2f4de9d3fa9517d8463e472b"
      )
      val block = tposBlock
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      matchBlock(block, jsonBlock)

      val jsonOwner = (jsonRewards \ "owner").as[JsValue]
      (jsonOwner \ "address")
        .as[String] mustEqual "Xi3sQfMQsy2CzMZTrnKW6HFGp1VqFThdLw"
      (jsonOwner \ "value").as[BigDecimal] mustEqual BigDecimal("22.275")

      val jsonMerchant = (jsonRewards \ "merchant").as[JsValue]
      (jsonMerchant \ "address")
        .as[String] mustEqual "XyJC8xnfFrHNcMinh6gxuPRYY9HCaY9DAo"
      (jsonMerchant \ "value").as[BigDecimal] mustEqual BigDecimal("0.225")

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address")
        .as[String] mustEqual "XydZnssXHCxxRtB4rk7evfKT9XP7GqyA9N"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "retrieve TPoS block with coinsplit" in {
      val tposBlock2 = BlockLoader.getRPC(
        "a3a9fb111a3f85c3d920c2dc58ce14d541a65763834247ef958aa3b4d665ef9c"
      )
      val block = tposBlock2
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      matchBlock(block, jsonBlock)

      val jsonOwner = (jsonRewards \ "owner").as[JsValue]
      (jsonOwner \ "address")
        .as[String] mustEqual "Xu5UkgRL8YRqoW6uEW8SxMLDkJwbjFVfge"
      (jsonOwner \ "value").as[BigDecimal] mustEqual BigDecimal("22.275")

      val jsonMerchant = (jsonRewards \ "merchant").as[JsValue]
      (jsonMerchant \ "address")
        .as[String] mustEqual "XbGFpsuhv6AH3gp3dx5eQrAexP5kESh9bY"
      (jsonMerchant \ "value").as[BigDecimal] mustEqual BigDecimal("0.225")

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address")
        .as[String] mustEqual "Xc3bKuGzy9grJZxC2ieTgQjjgyTMKSLqSM"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "retrieve the genesis block" in {
      val block = BlockLoader.getRPC(
        "00000c822abdbb23e28f79a49d29b41429737c6c7e15df40d1b1f1b35907ae34"
      )
      val response = GET(url(block.hash.string))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]

      matchBlock(block, jsonBlock)
    }

    "retrieve a block by height" in {
      val block = posBlock
      val response = GET(url(block.height.toString))

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val jsonBlock = (json \ "block").as[JsValue]
      val jsonRewards = (json \ "rewards").as[JsValue]

      matchBlock(block, jsonBlock)

      val jsonCoinstake = (jsonRewards \ "coinstake").as[JsValue]
      (jsonCoinstake \ "address")
        .as[String] mustEqual "XiHW7SR56UPHeXKwcpeVsE4nUfkHv5RqE3"
      (jsonCoinstake \ "value").as[BigDecimal] mustEqual BigDecimal(
        "22.49999999"
      )

      val jsonMasternode = (jsonRewards \ "masternode").as[JsValue]
      (jsonMasternode \ "address")
        .as[String] mustEqual "XjUDDq221NwqRtp85wfvoDrMaaxvUCDRrY"
      (jsonMasternode \ "value").as[BigDecimal] mustEqual BigDecimal("22.5")
    }

    "fail on the wrong blockhash format" in {
      val response = GET(
        url("000125c06cedf38b07bff174bdb61027935dbcb34831d28cff40bedb519d5")
      )

      status(response) mustEqual BAD_REQUEST

      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type")
        .as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
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

      (error \ "type")
        .as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "blockhash"
      (error \ "message").as[String].nonEmpty mustEqual true
    }

    "fail on an unknown block" in {
      val response = GET(
        url("000003dc4c2fc449dededaaad6efc33ce1b64b88a060652dc47edc63d6d6b524")
      )

      status(response) mustEqual BAD_REQUEST

      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type")
        .as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
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

  "GET /v2/blocks" should {
    "return the blockinfo list with no lastSeenHash" in {
      val block = BlockLoader.get(
        "00000267225f7dba55d9a3493740e7f0dde0f28a371d2c3b42e7676b5728d020"
      )
      val blockInfo =
        block
          .into[BlockInfo]
          .withFieldConst(_.transactions, 1)
          .transform

      val latestBlock = BlockLoader.get(
        "0000017ee4121cd8ae22f7321041ccb953d53828824217a9dc61a1c857facf85"
      )

      when(
        blockBlockingDataHandlerMock
          .getLatestBlock()
      ).thenReturn(Good(latestBlock))

      val expectedList = List(blockInfo);
      val result = Good(expectedList)

      when(
        blockBlockingDataHandlerMock
          .getBlocks(
            eqTo(Limit(1)),
            eqTo(OrderingCondition.DescendingOrder),
            eqTo(None)
          )
      ).thenReturn(result)

      val response = GET(s"/v2/blocks?limit=1")
      status(response) mustEqual OK

      val blockList = contentAsJson(response).as[List[JsValue]]
      blockList.size must be(1)

      val firstBlock = blockList.head
      matchBlockInfo(blockInfo, firstBlock)
    }

    "return the blockinfo list with lastSeenHash" in {
      val blockhash = Blockhash
        .from(
          "00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd"
        )
        .get

      val block = BlockLoader.get(
        "000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8"
      )
      val blockInfo =
        block
          .into[BlockInfo]
          .withFieldConst(_.transactions, 1)
          .transform

      val latestBlock = BlockLoader.get(
        "0000017ee4121cd8ae22f7321041ccb953d53828824217a9dc61a1c857facf85"
      )

      when(
        blockBlockingDataHandlerMock
          .getLatestBlock()
      ).thenReturn(Good(latestBlock))

      val expectedList = List(blockInfo);
      val result = Good(expectedList)

      when(
        blockBlockingDataHandlerMock
          .getBlocks(
            eqTo(Limit(1)),
            eqTo(OrderingCondition.DescendingOrder),
            eqTo(Some(blockhash))
          )
      ).thenReturn(result)

      val response = GET(s"/v2/blocks?lastSeenHash=${blockhash}&limit=1")
      status(response) mustEqual OK

      val blockList = contentAsJson(response).as[List[JsValue]]
      blockList.size must be(1)

      val firsrBlock = blockList.head
      matchBlockInfo(blockInfo, firsrBlock)
    }

    "return the medianTime" in {
      val blockhash = Blockhash.from("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd").get
      val block = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val latestBlock = BlockLoader.get("0000017ee4121cd8ae22f7321041ccb953d53828824217a9dc61a1c857facf85")
      val blockInfo = block
        .into[BlockInfo]
        .withFieldConst(_.transactions, 1)
        .transform

      when(blockBlockingDataHandlerMock.getLatestBlock()).thenReturn(Good(latestBlock))

      when(blockBlockingDataHandlerMock.getBlocks(Limit(1), OrderingCondition.DescendingOrder, Some(blockhash)))
        .thenReturn(Good(List(blockInfo)))

      val response = GET(s"/v2/blocks?lastSeenHash=$blockhash&limit=1")
      status(response) mustEqual OK

      val blockList = contentAsJson(response).as[List[JsValue]]
      blockList.size must be(1)

      (blockList.head \ "medianTime").as[Long] mustBe blockInfo.medianTime
    }

    "return the tposContract" in {
      val blockhash = Blockhash.from("00000766115b26ecbc09cd3a3db6870fdaf2f049d65a910eb2f2b48b566ca7bd").get
      val block = BlockLoader.get("000004645e2717b556682e3c642a4c6e473bf25c653ff8e8c114a3006040ffb8")
      val latestBlock = BlockLoader.get("0000017ee4121cd8ae22f7321041ccb953d53828824217a9dc61a1c857facf85")
      val blockInfo = block
        .into[BlockInfo]
        .withFieldConst(_.transactions, 1)
        .transform

      when(blockBlockingDataHandlerMock.getLatestBlock()).thenReturn(Good(latestBlock))

      when(blockBlockingDataHandlerMock.getBlocks(Limit(1), OrderingCondition.DescendingOrder, Some(blockhash)))
        .thenReturn(Good(List(blockInfo)))

      val response = GET(s"/v2/blocks?lastSeenHash=$blockhash&limit=1")
      status(response) mustEqual OK

      val blockList = contentAsJson(response).as[List[JsValue]]
      blockList.size must be(1)

      (blockList.head \ "tposContract").asOpt[TransactionId] mustBe blockInfo.tposContract
    }
  }

  "GET /v2/blocks/:blockhash/light-wallet-transactions" should {
    "return the transactions" in {
      val blockhash = Blockhash
        .from(
          "000003fb382f6892ae96594b81aa916a8923c70701de4e7054aac556c7271ef7"
        )
        .get
      val txid = DataGenerator.randomTransactionId
      val tx = Transaction.HasIO(
        transaction = Transaction(txid, blockhash, 0, Size(1)),
        inputs = List(
          Transaction.Input(
            fromTxid = DataGenerator.randomTransactionId,
            fromOutputIndex = 1,
            index = 0,
            value = 100,
            address = DataGenerator.randomAddress
          )
        ),
        outputs = List(
          Transaction.Output(
            txid = txid,
            index = 0,
            value = 200,
            address = DataGenerator.randomAddress,
            DataGenerator.randomHexString(8)
          )
        )
      )

      when(
        transactionDataHandlerMock
          .getTransactionsWithIOBy(
            eqTo(blockhash),
            eqTo(Limit(10)),
            eqTo(Option.empty)
          )
      ).thenReturn(Good(List(tx)))

      val response = GET(s"/v2/blocks/$blockhash/light-wallet-transactions")

      status(response) mustEqual OK

      val json = contentAsJson(response)
      val data = (json \ "data").as[List[JsValue]]
      data.size must be(1)

      val result = data.head
      (result \ "id").as[String] must be(txid.string)
      (result \ "size").as[Int] must be(tx.transaction.size.int)
      (result \ "time").as[Int] must be(tx.transaction.time)

      val inputs = (result \ "inputs").as[List[JsValue]]
      inputs.size must be(1)
      val input = inputs.head
      (input \ "txid").as[String] must be(tx.inputs.head.fromTxid.string)
      (input \ "index").as[Int] must be(tx.inputs.head.fromOutputIndex)

      val outputs = (result \ "outputs").as[List[JsValue]]
      outputs.size must be(1)
      val output = outputs.head
      (output \ "index").as[Int] must be(tx.outputs.head.index)
      (output \ "value").as[BigDecimal] must be(tx.outputs.head.value)
      (output \ "addresses").as[List[String]] must be(
        tx.outputs.head.addresses.map(_.string)
      )
      (output \ "script").as[String] must be(tx.outputs.head.script.string)
    }
  }

  "GET /blocks/headers" should {
    "return the headers" in {
      pending
    }

    "cache when all results from a query are delivered" in {
      pending
    }

    "not cache when all results from a query are delivered but one of the latest 20 blocks are included" in {
      pending
    }

    "not cache when retrieving headers in descending order" in {
      pending
    }
  }

  "GET /block-headers/:query" should {
    "return the blockHeader for the given blockhash" in {

      val block = BlockLoader.get(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0"
      )

      val blockheader =
        block
          .into[BlockHeader.Simple]
          .transform
          .withFilter(
            GolombCodedSet(1, 1, 1, List(new UnsignedByte(10.toByte)))
          )
      val result = Good(blockheader)

      when(
        blockBlockingDataHandlerMock
          .getHeader(eqTo(block.hash), eqTo(true))
      ).thenReturn(result)

      val response = GET(s"/block-headers/${block.hash}?includeFilter=true")
      status(response) mustEqual OK
      val json = contentAsJson(response)
      matchBlockHeader(blockheader, json)

      val cacheHeader = header("Cache-Control", response)
      cacheHeader.value mustEqual "public, max-age=31536000"
    }

    "return the blockHeader for the given height" in {

      val block = BlockLoader.get(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0"
      )

      val blockheader =
        block
          .into[BlockHeader.Simple]
          .transform
          .withFilter(
            GolombCodedSet(1, 1, 1, List(new UnsignedByte(10.toByte)))
          )
      val result = Good(blockheader)

      when(
        blockBlockingDataHandlerMock
          .getHeader(eqTo(block.height), eqTo(true))
      ).thenReturn(result)

      val response = GET(s"/block-headers/${block.height}?includeFilter=true")
      status(response) mustEqual OK
      val json = contentAsJson(response)
      matchBlockHeader(blockheader, json)

      val cacheHeader = header("Cache-Control", response)
      cacheHeader.value mustEqual "no-store"
    }

    "return the blockHeader for the given blockhash no filter" in {

      val block = BlockLoader.get(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0"
      )

      val blockheader =
        block
          .into[BlockHeader.Simple]
          .transform
      val result = Good(blockheader)

      when(
        blockBlockingDataHandlerMock
          .getHeader(eqTo(block.hash), eqTo(false))
      ).thenReturn(result)

      val response = GET(s"/block-headers/${block.hash}?includeFilter=false")
      status(response) mustEqual OK
      val json = contentAsJson(response)
      matchBlockHeader(blockheader, json)

      val cacheHeader = header("Cache-Control", response)
      cacheHeader.value mustEqual "public, max-age=31536000"
    }

    "return the blockHeader for the given height no filter" in {

      val block = BlockLoader.get(
        "1ca318b7a26ed67ca7c8c9b5069d653ba224bf86989125d1dfbb0973b7d6a5e0"
      )

      val blockheader =
        block
          .into[BlockHeader.Simple]
          .transform
      val result = Good(blockheader)

      when(
        blockBlockingDataHandlerMock
          .getHeader(eqTo(block.height), eqTo(false))
      ).thenReturn(result)

      val response = GET(s"/block-headers/${block.height}")
      status(response) mustEqual OK
      val json = contentAsJson(response)
      matchBlockHeader(blockheader, json)

      val cacheHeader = header("Cache-Control", response)
      cacheHeader.value mustEqual "no-store"
    }

  }

  "GET /blocks/:height/transactions/:txindex/lite" should {
    def url(height: Int, txindex: Int) =
      s"/v2/blocks/${height}/transactions/$txindex/lite"

    "retrieve lite transaction" in {
      val block = posBlock
      val response = GET(url(block.height.int, 0))

      status(response) mustEqual OK

      val expectedResult = Json.obj(
        "hex" -> "01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0502d8590101ffffffff0100000000000000000000000000",
        "blockhash" -> block.hash.string
      )
      val json = contentAsJson(response)
      json mustEqual expectedResult

      val cacheHeader = header("Cache-Control", response)
      cacheHeader.value mustEqual "public, max-age=31536000"
    }

    "fail when transaction on txindex does no exists" in {
      val block = posBlock
      val response = GET(url(block.height.int, 99))

      val json = contentAsJson(response)
      val errors = (json \ "errors").as[List[JsValue]]
      (errors.head \ "field").as[String] mustEqual "height"
      (errors.head \ "message").as[String] mustEqual "Transaction not found"
      (errors(1) \ "field").as[String] mustEqual "index"
      (errors(1) \ "message").as[String] mustEqual "Transaction not found"

      val cacheHeader = header("Cache-Control", response)
      cacheHeader mustBe None
    }

    "fail when block does no exists" in {
      val response = GET(url(123456789, 0))

      val json = contentAsJson(response)
      val errors = (json \ "errors").as[List[JsValue]]
      (errors.head \ "field").as[String] mustEqual "blockhash"
      (errors.head \ "message").as[String] mustEqual "Block not found"

      val cacheHeader = header("Cache-Control", response)
      cacheHeader mustBe None
    }
  }

  private def matchBlock(expected: Block.Canonical, actual: JsValue) = {
    val jsonBlock = actual
    val block = expected

    (jsonBlock \ "hash").as[Blockhash] mustEqual block.hash
    (jsonBlock \ "size").as[Size] mustEqual block.size
    (jsonBlock \ "bits").as[String] mustEqual block.bits
    (jsonBlock \ "chainwork").as[String] mustEqual block.chainwork
    (jsonBlock \ "difficulty").as[BigDecimal] mustEqual block.difficulty
    (jsonBlock \ "confirmations")
      .as[Confirmations] mustEqual block.confirmations
    (jsonBlock \ "height").as[Height] mustEqual block.height
    (jsonBlock \ "medianTime").as[Long] mustEqual block.medianTime
    (jsonBlock \ "time").as[Long] mustEqual block.time
    (jsonBlock \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
    (jsonBlock \ "version").as[Long] mustEqual block.version
    (jsonBlock \ "nonce").as[Int] mustEqual block.nonce
    (jsonBlock \ "previousBlockhash")
      .asOpt[Blockhash] mustEqual block.previousBlockhash
    (jsonBlock \ "nextBlockhash").asOpt[Blockhash] mustEqual block.nextBlockhash
  }

  private def matchBlockHeader(expected: BlockHeader, actual: JsValue) = {
    val jsonBlockHeader = actual
    val block = expected

    (jsonBlockHeader \ "hash").as[Blockhash] mustEqual block.hash
    (jsonBlockHeader \ "size").as[Size] mustEqual block.size
    (jsonBlockHeader \ "bits").as[String] mustEqual block.bits
    (jsonBlockHeader \ "chainwork").as[String] mustEqual block.chainwork
    (jsonBlockHeader \ "difficulty").as[BigDecimal] mustEqual block.difficulty
    (jsonBlockHeader \ "height").as[Height] mustEqual block.height
    (jsonBlockHeader \ "medianTime").as[Long] mustEqual block.medianTime
    (jsonBlockHeader \ "time").as[Long] mustEqual block.time
    (jsonBlockHeader \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
    (jsonBlockHeader \ "version").as[Long] mustEqual block.version
    (jsonBlockHeader \ "nonce").as[Int] mustEqual block.nonce
    (jsonBlockHeader \ "previousBlockhash")
      .asOpt[Blockhash] mustEqual block.previousBlockhash
    (jsonBlockHeader \ "nextBlockhash")
      .asOpt[Blockhash] mustEqual block.nextBlockhash

    block match {
      case BlockHeader.HasFilter(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            filter
          ) => {
        val jsonFilter = (jsonBlockHeader \ "filter").as[JsValue]

        (jsonFilter \ "n").as[Int] mustEqual filter.n
        (jsonFilter \ "m").as[Int] mustEqual filter.m
        (jsonFilter \ "p").as[Int] mustEqual filter.p
        (jsonFilter \ "hex").as[String] mustEqual filter.hex.string
      }
      case BlockHeader.Simple(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _) => ()
    }
  }

  private def matchBlockInfo(expected: BlockInfo, actual: JsValue) = {
    val jsonBlockInfo = actual
    val block = expected

    (jsonBlockInfo \ "hash").as[Blockhash] mustEqual block.hash
    (jsonBlockInfo \ "difficulty").as[BigDecimal] mustEqual block.difficulty
    (jsonBlockInfo \ "height").as[Height] mustEqual block.height
    (jsonBlockInfo \ "time").as[Long] mustEqual block.time
    (jsonBlockInfo \ "merkleRoot").as[Blockhash] mustEqual block.merkleRoot
    (jsonBlockInfo \ "previousBlockhash")
      .asOpt[Blockhash] mustEqual block.previousBlockhash
    (jsonBlockInfo \ "nextBlockhash")
      .asOpt[Blockhash] mustEqual block.nextBlockhash
  }

}
