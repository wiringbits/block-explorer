package com.xsn.explorer.services.validators

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.models.values.Address
import org.scalactic.{One, Or}

class AddressValidator {

  def validate(string: String): ApplicationResult[Address] = {
    val maybe = Address.from(string)
    Or.from(maybe, One(AddressFormatError))
  }
}
