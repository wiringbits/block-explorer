package com.xsn.explorer.models.values

import com.alexitc.playsonify.models.WrappedString

class IPAddress(val string: String) extends WrappedString

object IPAddress {
  private val pattern =
    "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$".r.pattern

  def from(string: String): Option[IPAddress] = {
    if (pattern.matcher(string).matches()) {
      Some(new IPAddress(string))
    } else {
      None
    }
  }
}
