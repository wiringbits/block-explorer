package com.xsn.explorer.errors

import com.alexitc.playsonify.models.{ErrorId, ServerError}

sealed trait CoinMarketCapError extends ServerError {
  val id = ErrorId.create

  override def cause: Option[Throwable] = None
}

case object CoinMarketCapUnexpectedResponseError extends CoinMarketCapError
case class CoinMarketCapRequestFailedError(status: Int)
    extends CoinMarketCapError
