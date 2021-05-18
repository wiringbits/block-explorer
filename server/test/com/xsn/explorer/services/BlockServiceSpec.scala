package com.xsn.explorer.services

import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.data.async.BlockFutureDataHandler
import com.xsn.explorer.helpers.{BlockLoader, FileBasedXSNService}
import com.xsn.explorer.models._
import com.xsn.explorer.parsers.OrderingConditionParser
import com.xsn.explorer.services.logic.{BlockLogic, TransactionLogic}
import com.xsn.explorer.services.validators.BlockhashValidator
import org.mockito.MockitoSugar._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class BlockServiceSpec extends AsyncWordSpecLike with Matchers {

  def createService(): BlockService = {
    val xsnService = new FileBasedXSNService()
    val blockDataHandler = mock[BlockFutureDataHandler]
    val paginatedQueryValidator = new PaginatedQueryValidator()
    val blockhashValidator = new BlockhashValidator()
    val blockLogic = new BlockLogic()
    val transactionLogic = new TransactionLogic()
    val orderingConditionParser = new OrderingConditionParser()

    new BlockService(
      xsnService,
      blockDataHandler,
      paginatedQueryValidator,
      blockhashValidator,
      blockLogic,
      transactionLogic,
      orderingConditionParser
    )
  }

  "getBlockRewards" should {

    "get correct rewards for block 1663808" in {
      val blockHash = "1277dd29797e7687d0390de275123690d403796022b22873caa8ff3c0e280159"
      val block = BlockLoader.getFullRPC(blockHash)

      block.height.int mustBe 1663808

      val service = createService()
      service.getBlockRewards(block, BlockExtractionMethod.ProofOfStake).map { rewards =>
        rewards.get match {
          case PoSBlockRewards(coinstake, Some(masternode), stakedAmount, stakedDuration) =>
            coinstake.value mustBe BigDecimal(8.89999999)
            coinstake.address.string mustBe "7bs9TobPv6k9vvZVwYHEVwUQzv2t3MQcWD"

            masternode.value mustBe BigDecimal(9)
            masternode.address.string mustBe "XuUzwDzqwYynjGfmMYY9ba3nzt6xTPjsE6"

            stakedAmount mustBe BigDecimal(182513.58176347)
            stakedDuration mustBe 7897385

          case _ =>
            fail()
        }
      }
    }

    "get correct rewards for block 1531049" in {
      val blockHash = "0921e87173c79a3639c1c05a119ede932e7757414a9a58fed34b5508d3d30fde"
      val block = BlockLoader.getFullRPC(blockHash)

      block.height.int mustBe 1531049

      val service = createService()
      service.getBlockRewards(block, BlockExtractionMethod.ProofOfStake).map { rewards =>
        rewards.get match {
          case PoSBlockRewards(coinstake, Some(masternode), stakedAmount, stakedDuration) =>
            coinstake.value mustBe BigDecimal(9)
            coinstake.address.string mustBe "7bs9TobPv6k9vvZVwYHEVwUQzv2t3MQcWD"

            masternode.value mustBe BigDecimal(9)
            masternode.address.string mustBe "XjP23TKrSpw1yraSeGaenv7ce5dVNiKYSZ"

            stakedAmount mustBe BigDecimal(120000)
            stakedDuration mustBe 177358

          case _ =>
            fail()
        }
      }
    }

    "get correct rewards for block 1671361" in {
      val blockHash = "f0f1fbc98821947b4e869bb234647fcd31385f04f5f7dc9d136d8e6be47ecc19"
      val block = BlockLoader.getFullRPC(blockHash)

      block.height.int mustBe 1671361

      val service = createService()
      service.getBlockRewards(block, BlockExtractionMethod.ProofOfStake).map { rewards =>
        rewards.get match {
          case PoSBlockRewards(coinstake, Some(masternode), stakedAmount, stakedDuration) =>
            coinstake.value mustBe BigDecimal(9)
            coinstake.address.string mustBe "Xi1BEVHFRYg1QQfrNJF8L9yXvREAADxCLk"

            masternode.value mustBe BigDecimal(9)
            masternode.address.string mustBe "Xi53MGFpPYZN5rFeKqGk3WSuNk7CGfQ4cj"

            stakedAmount mustBe BigDecimal(2112)
            stakedDuration mustBe 4965844

          case _ =>
            fail()
        }
      }
    }

    "get correct rewards for block 1671552" in {
      val blockHash = "4144179ee535329b010adbe0d1afedfadcbd39d748617b163a42b528ce31430d"
      val block = BlockLoader.getFullRPC(blockHash)

      block.height.int mustBe 1671552

      val service = createService()
      service.getBlockRewards(block, BlockExtractionMethod.TrustlessProofOfStake).map { rewards =>
        rewards.get match {
          case TPoSBlockRewards(owner, merchant, Some(masternode), stakedAmount, stakedDuration) =>
            owner.value mustBe BigDecimal(8.91)
            owner.address.string mustBe "XkMPfG7CdAa4i9zjoxzLsJdtT63f1o1UyK"

            merchant.value mustBe BigDecimal(0.09)
            merchant.address.string mustBe "XhJmngaMHdbJfHA5fXdSLqgi5V5yUnsuQL"

            masternode.value mustBe BigDecimal(9)
            masternode.address.string mustBe "XhJhs36UX8f3ChxFuCsUrr4aSE95BSNkrx"

            stakedAmount mustBe BigDecimal(5288.63815864)
            stakedDuration mustBe 148079

          case _ =>
            fail()
        }
      }
    }
  }
}
