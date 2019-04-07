package com.xsn.explorer.services.validators

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.TransactionFormatError
import com.xsn.explorer.models.values.TransactionId

class TransactionIdValidator {

  def validate(string: String): ApplicationResult[TransactionId] = {
    optional(string, TransactionFormatError)(TransactionId.from)
  }
}
