package com.xsn.explorer.services.validators

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.{PaginatedQueryLimitError, PaginatedQueryOffsetError}
import com.xsn.explorer.models.base.{Limit, Offset, PaginatedQuery}
import org.scalactic.{Accumulation, Bad, Good}

class PaginatedQueryValidator {

  private val MinOffset = 0
  private val LimitRange = 1 to 100

  def validate(query: PaginatedQuery): ApplicationResult[PaginatedQuery] = {
    Accumulation.withGood(
      validateOffset(query.offset),
      validateLimit(query.limit)) {

      PaginatedQuery.apply
    }
  }

  private def validateOffset(offset: Offset): ApplicationResult[Offset] = {
    if (offset.int >= MinOffset) {
      Good(offset)
    } else {
      Bad(PaginatedQueryOffsetError).accumulating
    }
  }

  private def validateLimit(limit: Limit): ApplicationResult[Limit] = {
    if (LimitRange contains limit.int) {
      Good(limit)
    } else {
      Bad(PaginatedQueryLimitError(LimitRange.last)).accumulating
    }
  }
}
