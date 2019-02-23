package controllers

import com.alexitc.playsonify.models.ordering.OrderingQuery
import com.alexitc.playsonify.models.pagination.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.models.values.Height
import com.xsn.explorer.services.{BlockService, TransactionService}
import controllers.common.{Codecs, MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject

import scala.util.Try

class BlocksController @Inject() (
    blockService: BlockService,
    transactionService: TransactionService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Codecs._

  def getLatestBlocks() = public { _ =>
    blockService.getLatestBlocks()
  }

  def getBlockHeaders(lastSeenHash: Option[String], limit: Int) = public { _ =>
    blockService.getBlockHeaders(Limit(limit), lastSeenHash)
  }

  /**
   * Try to retrieve a block by height, in case the query argument
   * is not a valid height, we assume it might be a blockhash and try to
   * retrieve the block by blockhash.
   */
  def getDetails(query: String) = public { _ =>
    Try(query.toInt)
        .map(Height.apply)
        .map(blockService.getDetails)
        .getOrElse(blockService.getDetails(query))
  }

  def getRawBlock(query: String) = public { _ =>
    Try(query.toInt)
        .map(Height.apply)
        .map(blockService.getRawBlock)
        .getOrElse(blockService.getRawBlock(query))
  }

  def getTransactions(blockhash: String, offset: Int, limit: Int, orderBy: String) = public { _ =>
    val query = PaginatedQuery(Offset(offset), Limit(limit))
    val ordering = OrderingQuery(orderBy)
    transactionService.getByBlockhash(blockhash, query, ordering)
  }

  def getTransactionsV2(blockhash: String, limit: Int, lastSeenTxid: Option[String]) = public { _ =>
    transactionService.getByBlockhash(blockhash, Limit(limit), lastSeenTxid)
  }
}
