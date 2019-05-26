package com.xsn.explorer.cache

import com.alexitc.playsonify.core.FutureApplicationResult
import com.alexitc.playsonify.core.FutureOr.Implicits.FutureOps
import com.alexitc.playsonify.models.ordering.OrderingCondition
import com.alexitc.playsonify.models.pagination.Limit
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.xsn.explorer.models.WrappedResult
import com.xsn.explorer.models.persisted.BlockHeader
import com.xsn.explorer.models.values.Blockhash
import org.scalactic.Good
import play.api.libs.json.{JsValue, Json, Writes}

import scala.concurrent.{ExecutionContext, Future}

/**
 * This cache aims to provide fast access to the block headers data used for syncing the light wallet.
 *
 * The ideal strategy is to cache an entry and never expire it because most headers are unlikely to change.
 *
 * In order to handle chain reorganizations, the last entry must not be cached (TODO).
 *
 * Currently, the entries are expired after some minutes just to keep the last entry updated.
 *
 * Also, it is faster to cache the JSON data that is sent in a response instead of being encoding it on each request.
 */
class BlockHeaderCache(cache: Cache[BlockHeaderCache.Key, BlockHeaderCache.EncodedValue]) {

  import BlockHeaderCache._

  def getOrSet(key: Key, entrySize: Int)(
      f: => FutureApplicationResult[Value]
  )(implicit ec: ExecutionContext, writes: Writes[Value]): FutureApplicationResult[EncodedValue] = {

    if (isCacheable(key, entrySize)) {
      get(key)
        .map(v => Future.successful(Good(v)))
        .getOrElse {
          val result = for {
            r <- f.toFutureOr
          } yield (r.data.size, Json.toJson(r))

          // set cache only if the response is complete
          val _ = result.map {
            case (size, json) =>
              if (size == key.limit.int) {
                cache.put(key, json)
              }
          }

          result.map(_._2).toFuture
        }
    } else {
      val result = for {
        r <- f.toFutureOr
      } yield Json.toJson(r)

      result.toFuture
    }
  }

  /**
   * A block header list is cacheable if all the following meet:
   * - The ordering is from oldest to newest
   * - The limit matches the expected entry size
   * - The entry is not the last one (TODO)
   */
  def isCacheable(key: Key, entrySize: Int): Boolean = {
    key.orderingCondition == OrderingCondition.AscendingOrder &&
    key.limit.int == entrySize
  }

  def get(key: Key): Option[EncodedValue] = {
    Option(cache.getIfPresent(key))
  }
}

object BlockHeaderCache {

  case class Key(limit: Limit, orderingCondition: OrderingCondition, lastSeenHash: Option[Blockhash])

  type Value = WrappedResult[List[BlockHeader]]
  type EncodedValue = JsValue

  def default: BlockHeaderCache = {
    val cache = Caffeine
      .newBuilder()
      .maximumSize(250000)
      .build[Key, EncodedValue]

    new BlockHeaderCache(cache)
  }
}
