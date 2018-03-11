package com.xsn.explorer.models

class TransactionId private (val string: String) extends AnyVal

object TransactionId {

  private val pattern = "^[a-f0-9]{64}$".r.pattern

  def from(string: String): Option[TransactionId] = {
    val lowercaseString = string.toLowerCase

    if (pattern.matcher(lowercaseString).matches()) {
      Some(new TransactionId(lowercaseString))
    } else {
      None
    }
  }
}
