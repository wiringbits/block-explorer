package com.xsn.explorer.services.validators

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.TransactionError
import com.xsn.explorer.models.values.TransactionId

class TransactionIdValidator {

  def validate(string: String): ApplicationResult[TransactionId] = {
    optional(string, TransactionError.InvalidFormat)(TransactionId.from)
  }
}
