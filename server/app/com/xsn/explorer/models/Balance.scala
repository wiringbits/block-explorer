package com.xsn.explorer.models

case class Balance(
    address: Address,
    received: BigDecimal = BigDecimal(0),
    spent: BigDecimal = BigDecimal(0)) {

  def available: BigDecimal = received - spent
}
