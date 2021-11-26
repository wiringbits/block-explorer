package controllers

import com.alexitc.playsonify.core.ApplicationResult
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.models.pagination.Limit
import com.alexitc.playsonify.play.PublicErrorRenderer
import com.xsn.explorer.data.{BlockBlockingDataHandler, TransactionBlockingDataHandler}
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.helpers.DataGenerator.{randomBlockHeader, randomBlockhash, randomTransactionId}
import com.xsn.explorer.helpers.{DataHelper, FileBasedXSNService, TransactionDummyDataHandler, TransactionLoader}
import com.xsn.explorer.models._
import com.xsn.explorer.models.persisted.Transaction
import com.xsn.explorer.models.values._
import com.xsn.explorer.services.XSNService
import controllers.common.MyAPISpec
import org.mockito.MockitoSugar.{mock, when}
import org.scalactic.{Bad, Good}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._

class TransactionsControllerSpec extends MyAPISpec {

  import DataHelper._

  private val coinbaseTx = TransactionLoader.get(
    "024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c"
  )

  private val nonCoinbaseTx = TransactionLoader.get(
    "0834641a7d30d8a2d2b451617599670445ee94ed7736e146c13be260c576c641"
  )

  private val severalInputsTx =
    TransactionLoader.get(
      "a3c43d22bbba31a6e5c00f565cb9c5a1a365407df4cc90efa8a865656b52c0eb"
    )

  private val transactionList = List(
    TransactionInfo(
      createTransactionId(
        "92c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"
      ),
      createBlockhash(
        "ad22f0dcea2fdaa357aac6eab00695cf07b487e34113598909f625c24629c981"
      ),
      12312312L,
      Size(1000),
      sent = 50,
      received = 200,
      height = Height(2)
    ),
    TransactionInfo(
      createTransactionId(
        "0c0f595a004eab5cf62ea70570f175701d120a0da31c8222d2d99fc60bf96577"
      ),
      createBlockhash(
        "e2df117061eb6ed4d2832616dd7a5f07b01ad3d148c9d7f9a1628d339d6caedb"
      ),
      1521700630L,
      Size(1000),
      sent = 100,
      received = 50,
      height = Height(1)
    )
  )

  private val spentTransaction = "0c0f595a004eab5cf62ea70570f175701d120a0da31c8222d2d99fc60bf96577"
  private val spendingTransaction = Transaction(
    id = createTransactionId("a3c43d22bbba31a6e5c00f565cb9c5a1a365407df4cc90efa8a865656b52c0eb"),
    blockhash = randomBlockhash,
    time = java.lang.System.currentTimeMillis(),
    size = Size(1000)
  )

  private val customXSNService = new FileBasedXSNService

  private val transactionDataHandler = new TransactionDummyDataHandler {

    override def getOutput(
        txid: TransactionId,
        index: Int
    ): ApplicationResult[persisted.Transaction.Output] = {
      Bad(TransactionError.OutputNotFound(txid, index)).accumulating
    }

    override def get(
        limit: Limit,
        lastSeenTxid: Option[TransactionId],
        orderingCondition: OrderingCondition,
        includeZeroValueTransactions: Boolean
    ): ApplicationResult[List[TransactionInfo]] = {
      if (lastSeenTxid == None) {
        Good(transactionList)
      } else {
        Good(List(transactionList.last))
      }
    }

    override def getSpendingTransaction(
        txid: TransactionId,
        outputIndex: Int
    ): ApplicationResult[Option[Transaction]] = {
      if (txid.string == spentTransaction) {
        Good(Some(spendingTransaction))
      } else {
        Good(None)
      }
    }
  }

  private val blockDataHandler = mock[BlockBlockingDataHandler]

  override val application = guiceApplicationBuilder
    .overrides(bind[XSNService].to(customXSNService))
    .overrides(bind[TransactionBlockingDataHandler].to(transactionDataHandler))
    .overrides(bind[BlockBlockingDataHandler].to(blockDataHandler))
    .build()

  "GET /transactions/:txid" should {
    def url(txid: String) = s"/transactions/$txid"

    "return coinbase transaction" in {
      val tx = coinbaseTx
      val response = GET(url(tx.id.string))

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "id").as[String] mustEqual tx.id.string
      (json \ "blockhash").as[String] mustEqual tx.blockhash.string
      (json \ "size").as[Size] mustEqual tx.size
      (json \ "time").as[Long] mustEqual tx.time
      (json \ "blocktime").as[Long] mustEqual tx.blocktime
      (json \ "confirmations").as[Confirmations] mustEqual tx.confirmations

      val outputJsonList = (json \ "output").as[List[JsValue]]
      outputJsonList.size mustEqual 1

      val outputJson = outputJsonList.head
      (outputJson \ "address").as[String] mustEqual tx.vout.head.addresses
        .flatMap(_.headOption)
        .get
        .string
      (outputJson \ "value").as[BigDecimal] mustEqual tx.vout.head.value
    }

    "return non-coinbase transaction" in {
      val input =
        List(
          TransactionValue(
            createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"),
            BigDecimal("2343749.965625"),
            HexString.from("00").get
          )
        ).map { v =>
          rpc.TransactionVIN.HasValues(
            createTransactionId(
              "024aba1d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c"
            ),
            0,
            v.value,
            v.addresses,
            v.pubKeyScript
          )
        }

      val tx = nonCoinbaseTx.copy(vin = input)

      val details = TransactionDetails.from(tx)
      val response = GET(url(tx.id.string))

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "id").as[String] mustEqual tx.id.string
      (json \ "blockhash").as[String] mustEqual tx.blockhash.string
      (json \ "size").as[Size] mustEqual tx.size
      (json \ "time").as[Long] mustEqual tx.time
      (json \ "blocktime").as[Long] mustEqual tx.blocktime
      (json \ "confirmations").as[Confirmations] mustEqual tx.confirmations

      val inputJsonList = (json \ "input").as[List[JsValue]]
      inputJsonList.size mustEqual 1

      val inputJson = inputJsonList.head
      (inputJson \ "address").as[String] mustEqual details.input.head.address
        .map(_.string)
        .getOrElse("")
      (inputJson \ "value").as[BigDecimal] mustEqual details.input.head.value

      val outputJsonList = (json \ "output").as[List[JsValue]]
      outputJsonList.size mustEqual 3

      val outputJson2 = outputJsonList.drop(1).head
      (outputJson2 \ "address").as[String] mustEqual details.output
        .drop(1)
        .head
        .address
        .map(_.string)
        .getOrElse("")
      (outputJson2 \ "value")
        .as[BigDecimal] mustEqual details.output.drop(1).head.value

      val outputJson3 = outputJsonList.drop(2).head
      (outputJson3 \ "address").as[String] mustEqual details.output
        .drop(2)
        .head
        .address
        .map(_.string)
        .getOrElse("")
      (outputJson3 \ "value")
        .as[BigDecimal] mustEqual details.output.drop(2).head.value
    }

    "return a transaction with several inputs" in {
      val tx = severalInputsTx
      val inputValue =
        TransactionValue(
          createAddress("XgEGH3y7RfeKEdn2hkYEvBnrnmGBr7zvjL"),
          BigDecimal("73242.18642578"),
          HexString.from("00").get
        )

      val response = GET(url(tx.id.string))

      status(response) mustEqual OK
      val json = contentAsJson(response)
      (json \ "id").as[String] mustEqual tx.id.string
      (json \ "blockhash").as[String] mustEqual tx.blockhash.string
      (json \ "size").as[Size] mustEqual tx.size
      (json \ "time").as[Long] mustEqual tx.time
      (json \ "blocktime").as[Long] mustEqual tx.blocktime
      (json \ "confirmations").as[Confirmations] mustEqual tx.confirmations

      val inputJsonList = (json \ "input").as[List[JsValue]]
      inputJsonList.size mustEqual 11

      inputJsonList.foreach { inputJson =>
        (inputJson \ "address")
          .as[String] mustEqual inputValue.address.map(_.string).getOrElse("")
        (inputJson \ "value").as[BigDecimal] mustEqual inputValue.value
      }

      val outputJsonList = (json \ "output").as[List[JsValue]]
      outputJsonList.size mustEqual 2

      val outputJson = outputJsonList.head
      (outputJson \ "address")
        .as[String] mustEqual "XcmqLX4qptMgAigXTVH4SJRVb6ZKmq8rjH"
      (outputJson \ "value").as[BigDecimal] mustEqual BigDecimal(
        "55664.05066658"
      )

      val outputJson2 = outputJsonList.drop(1).head
      (outputJson2 \ "address")
        .as[String] mustEqual "XvUAd4vQtFtZ7v2Uo8e6aSsiRLAwyq1jwb"
      (outputJson2 \ "value").as[BigDecimal] mustEqual BigDecimal(
        "750000.00000000"
      )
    }

    "fail on wrong transaction format" in {
      // 63 characters
      val txid =
        "000001d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c"
      val response = GET(url(txid))

      status(response) mustEqual BAD_REQUEST
      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type")
        .as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "transactionId"
      (error \ "message").as[String].nonEmpty mustEqual true
    }

    "fail on unknown transaction" in {
      val txid =
        "0000001d535cfe5dd3ea465d46a828a57b00e1df012d7a2d158e0f7484173f7c"
      val response = GET(url(txid))

      status(response) mustEqual BAD_REQUEST
      val json = contentAsJson(response)
      val errorList = (json \ "errors").as[List[JsValue]]

      errorList.size mustEqual 1
      val error = errorList.head

      (error \ "type")
        .as[String] mustEqual PublicErrorRenderer.FieldValidationErrorType
      (error \ "field").as[String] mustEqual "transactionId"
      (error \ "message").as[String].nonEmpty mustEqual true
    }
  }

  "GET transactions/:txid/raw" should {
    def url(txid: String) = s"/transactions/$txid/raw"

    "retrieve the raw transaction" in {
      val tx = coinbaseTx
      val expected = TransactionLoader.json(tx.id.string)
      val response = GET(url(tx.id.string))

      status(response) mustEqual OK
      val json = contentAsJson(response)

      json mustEqual expected
    }
  }

  "GET /transactions" should {
    "return the last transactions without lastSeenTxid" in {
      val response = GET("/transactions?limit=2")

      status(response) mustEqual OK
      val json = contentAsJson(response)
      val data = (json \ "data").as[List[JsValue]]
      data.size mustEqual 2

      val item = data.head
      val expected = transactionList.head
      (item \ "id").as[String] mustEqual expected.id.string
      (item \ "blockhash").as[String] mustEqual expected.blockhash.string
      (item \ "time").as[Long] mustEqual expected.time
      (item \ "size").as[Int] mustEqual expected.size.int
      (item \ "sent").as[BigDecimal] mustEqual expected.sent
      (item \ "received").as[BigDecimal] mustEqual expected.received
      (item \ "height").as[Int] mustEqual expected.height.int
    }

    "return the transactions with lastSeenTxid" in {
      val response =
        GET(
          "/transactions?limit=1&lastSeenTxid=92c51e4fe89466faa734d6207a7ef6115fa1dd33f7156b006fafc6bb85a79eb8"
        )

      status(response) mustEqual OK
      val json = contentAsJson(response)
      val data = (json \ "data").as[List[JsValue]]
      data.size mustEqual 1

      val item = data.head
      val expected = transactionList.last
      (item \ "id").as[String] mustEqual expected.id.string
      (item \ "blockhash").as[String] mustEqual expected.blockhash.string
      (item \ "time").as[Long] mustEqual expected.time
      (item \ "size").as[Int] mustEqual expected.size.int
      (item \ "sent").as[BigDecimal] mustEqual expected.sent
      (item \ "received").as[BigDecimal] mustEqual expected.received
      (item \ "height").as[Int] mustEqual expected.height.int
    }
  }

  "GET transactions/:txid/lite" should {
    def url(txid: String) = s"/transactions/$txid/lite"

    "return transaction lite" in {
      val tx = nonCoinbaseTx
      val response = GET(url(tx.id.string))

      status(response) mustEqual OK
      val json = contentAsJson(response)

      val txHex =
        "0100000001d967897603771672654db507a02ceb65dea8a682d2333ee819cac80950ec5c58020000006a473044022059a0cc21ad24ae18726d128c85328a0b54dab62aeb41ffbcad368ece6fdf9d2602200e477332401ce1296d379dc5f797720e854e40fc5af0a268f585e7dae64d38e5012103624fbfb0079e85bbc9aaeba6f48581326ad01194b3c54ce22852a27b1d2892d1ffffffff03000000000000000000220935d7946a00001976a9143cc9ede1da2d7351aaebaf6a25d2657e0b05a71688ac220935d7946a00001976a9143cc9ede1da2d7351aaebaf6a25d2657e0b05a71688ac00000000"
      (json \ "hex").as[String] mustEqual txHex
      (json \ "blockhash").as[Blockhash] mustEqual tx.blockhash
      (json \ "index").as[Int] mustEqual 1
      (json \ "height").as[Height] mustEqual Height(809)

      val cacheHeader = header("Cache-Control", response)
      cacheHeader.value mustEqual "public, max-age=31536000"
    }
    "return recent transaction lite" in {
      pending
    }
    "bad txid format" in {
      val response = GET(
        url("0834641a7d30d8a2d2b451617599670445ee94ed7736e146c13be260c576c6")
      )

      status(response) mustEqual BAD_REQUEST
      val json = contentAsJson(response)

      val errors = (json \ "errors").as[List[JsValue]]
      (errors.head \ "field").as[String] mustEqual "transactionId"
      (errors.head \ "message")
        .as[String] mustEqual "Invalid transaction format"

      val cacheHeader = header("Cache-Control", response)
      cacheHeader mustBe None
    }

    "when transaction doesn't exists" in {
      val response = GET(
        url("a3c43d22bbba31a6e5c00f465cb9c5a1a365407df4cc90efa8a865656b52c1ec")
      )

      status(response) mustEqual BAD_REQUEST
      val json = contentAsJson(response)

      val errors = (json \ "errors").as[List[JsValue]]
      (errors.head \ "field").as[String] mustEqual "transactionId"
      (errors.head \ "message").as[String] mustEqual "Transaction not found"

      val cacheHeader = header("Cache-Control", response)
      cacheHeader mustBe None
    }
  }

  "GET /transactions/:txid/utxos/:index/spending-transaction" should {
    "return the spending information when output has been spent" in {
      val header = randomBlockHeader()
      when(blockDataHandler.getHeader(spendingTransaction.blockhash, false)).thenReturn(Good(header))

      val response = GET(s"/transactions/$spentTransaction/utxos/0/spending-transaction")
      val json = contentAsJson(response)

      status(response) mustEqual OK
      (json \ "rawTransaction").as[JsValue] mustBe TransactionLoader.json(spendingTransaction.id.string)
      (json \ "height").as[Int] mustBe header.height.int
    }

    "return empty JSON when output has not been spent" in {
      val response = GET(s"/transactions/$randomTransactionId/utxos/0/spending-transaction")
      val json = contentAsJson(response)

      status(response) mustEqual OK
      json mustBe Json.parse("{}")
    }
  }

  "POST   /tposcontracts/encode" should {
    val url = s"/tposcontracts/encode"

    "return the tpos contract encoded" in {
      val params: String =
        s"""
           |{
           |    "tposAddress" : "XpLy7iJebcUbpmsH1PAiHRn8BrrMdw73KV",
           |    "merchantAddress" : "XqzYHcK3STW5F22S7kep7dMU4sx3SKFMBv",
           |    "commission" : 10,
           |    "signature" : "201F2D052FB372248F89F9F2C9106BE9A670D5538C01E4F39215C92717B847D3EA2466E7D1D88010FF98996913ED024DDE8EBC860984F7806E5619C88CABF2EF06"
           |}
           |""".stripMargin

      val response = POST(url, Some(params))
      status(response) mustEqual OK
      val json = contentAsJson(response)

      val contract =
        "020000000a001976a91495cf859d7a40c5d7fded2a03cb8d7dcf307eab1188ac1976a914a7e2ba4e0d91273d686f446fa04ca5fe800d452d88ac41201f2d052fb372248f89f9f2c9106be9a670d5538c01e4f39215c92717b847d3ea2466e7d1d88010ff98996913ed024dde8ebc860984f7806e5619c88cabf2ef06"
      (json \ "tposContractEncoded").as[String] mustEqual contract

    }
  }
}
