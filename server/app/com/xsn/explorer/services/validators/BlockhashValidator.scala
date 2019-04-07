package com.xsn.explorer.services.validators

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.BlockhashFormatError
import com.xsn.explorer.models.values.Blockhash

class BlockhashValidator {

  def validate(string: String): ApplicationResult[Blockhash] = {
    optional(string, BlockhashFormatError)(Blockhash.from)
  }
}
