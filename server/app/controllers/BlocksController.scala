package controllers

import javax.inject.Inject

import com.xsn.explorer.services.BlockService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class BlocksController @Inject() (
    blockService: BlockService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getLatestBlocks() = publicNoInput { _ =>
    blockService.getLatestBlocks()
  }

  def getDetails(blockhash: String) = publicNoInput { _ =>
    blockService.getDetails(blockhash)
  }
}
