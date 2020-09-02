package com.xsn.explorer.services

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{FutureApplicationResult, FuturePaginatedResult}
import com.alexitc.playsonify.models.ordering.{FieldOrdering, OrderingCondition, OrderingQuery}
import com.alexitc.playsonify.models.pagination.{Count, PaginatedQuery, PaginatedResult}
import com.alexitc.playsonify.validators.PaginatedQueryValidator
import com.xsn.explorer.errors.{IPAddressFormatError, MerchantnodeNotFoundError}
import com.xsn.explorer.models.fields.MerchantnodeField
import com.xsn.explorer.models.rpc.Merchantnode
import com.xsn.explorer.models.values.IPAddress
import com.xsn.explorer.parsers.MerchantnodeOrderingParser
import com.xsn.explorer.services.synchronizer.repository.MerchantnodeRepository
import javax.inject.Inject
import org.scalactic.{Bad, Good}

import scala.concurrent.ExecutionContext

class MerchantnodeService @Inject()(
    queryValidator: PaginatedQueryValidator,
    merchantnodeOrderingParser: MerchantnodeOrderingParser,
    merchantnodeRepository: MerchantnodeRepository
)(implicit ec: ExecutionContext) {

  private val maxTransactionsPerQuery = 2000
  
  def getMerchantnodes(
      paginatedQuery: PaginatedQuery,
      orderingQuery: OrderingQuery
  ): FuturePaginatedResult[Merchantnode] = {
    val result = for {
      validatedQuery <- queryValidator.validate(paginatedQuery, maxTransactionsPerQuery).toFutureOr
      ordering <- merchantnodeOrderingParser.from(orderingQuery).toFutureOr
      merchantnodes <- merchantnodeRepository.getAll().map(Good(_)).toFutureOr
    } yield build(merchantnodes, validatedQuery, ordering)

    result.toFuture
  }

  def getMerchantnode(ipAddressString: String): FutureApplicationResult[Merchantnode] = {
    val result = for {
      ipAddress <- IPAddress
        .from(ipAddressString)
        .map(Good(_))
        .getOrElse(Bad(IPAddressFormatError).accumulating)
        .toFutureOr

      merchantnode <- merchantnodeRepository
        .find(ipAddress)
        .map {
          case Some(x) => Good(x)
          case None => Bad(MerchantnodeNotFoundError).accumulating
        }
        .toFutureOr
    } yield merchantnode

    result.toFuture
  }

  private def build(list: List[Merchantnode], query: PaginatedQuery, ordering: FieldOrdering[MerchantnodeField]) = {      
    val partial = sort(list, ordering)
      .slice(query.offset.int, query.offset.int + query.limit.int)

    PaginatedResult(query.offset, query.limit, Count(list.size), partial)
  }

  private def sort(list: List[Merchantnode], ordering: FieldOrdering[MerchantnodeField]) = {
    val sorted = sortByField(list, ordering.field)
    applyOrderingCondition(sorted, ordering.orderingCondition)
  }

  private def sortByField(list: List[Merchantnode], field: MerchantnodeField) = field match {
    case MerchantnodeField.ActiveSeconds => list.sortBy(_.activeSeconds)
    case MerchantnodeField.IP => list.sortBy(_.ip)
    case MerchantnodeField.LastSeen => list.sortBy(_.lastSeen)
    case MerchantnodeField.Status => list.sortBy(_.status)
  }

  private def applyOrderingCondition[A](list: List[A], orderingCondition: OrderingCondition) = orderingCondition match {
    case OrderingCondition.AscendingOrder => list
    case OrderingCondition.DescendingOrder => list.reverse
  }
}
