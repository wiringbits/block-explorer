package controllers

import javax.inject.Inject

import com.xsn.explorer.models.Height
import com.xsn.explorer.services.BlockService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

import scala.util.Try

class BlocksController @Inject() (
    blockService: BlockService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getLatestBlocks() = publicNoInput { _ =>
    blockService.getLatestBlocks()
  }

  /**
   * Try to retrieve a block by height, in case the query argument
   * is not a valid height, we assume it might be a blockhash and try to
   * retrieve the block by blockhash.
   */
  def getDetails(query: String) = publicNoInput { _ =>
    Try(query.toInt)
        .map(Height.apply)
        .map(blockService.getDetails)
        .getOrElse(blockService.getDetails(query))
  }

  def getRawBlock(query: String) = publicNoInput { _ =>
    Try(query.toInt)
        .map(Height.apply)
        .map(blockService.getRawBlock)
        .getOrElse(blockService.getRawBlock(query))
  }
}
