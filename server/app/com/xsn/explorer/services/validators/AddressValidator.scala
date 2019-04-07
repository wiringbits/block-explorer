package com.xsn.explorer.services.validators

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.models.values.Address

class AddressValidator {

  def validate(string: String): ApplicationResult[Address] = {
    optional(string, AddressFormatError)(Address.from)
  }
}
