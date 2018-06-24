package com.xsn.explorer.services

import javax.inject.Inject

import com.alexitc.playsonify.core.FutureOr.Implicits.{FutureOps, OrOps}
import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult}
import com.xsn.explorer.data.async.BalanceFutureDataHandler
import com.xsn.explorer.errors.AddressFormatError
import com.xsn.explorer.models.{Address, Balance}
import org.scalactic.{One, Or}
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

class AddressService @Inject() (
    xsnService: XSNService,
    balanceFutureDataHandler: BalanceFutureDataHandler)(
    implicit ec: ExecutionContext) {

  def getBy(addressString: String): FutureApplicationResult[Balance] = {
    val result = for {
      address <- getAddress(addressString).toFutureOr
      balance <- balanceFutureDataHandler.getBy(address).toFutureOr
    } yield balance

    result.toFuture
  }

  def getUnspentOutputs(addressString: String): FutureApplicationResult[JsValue] = {
    val result = for {
      address <- getAddress(addressString).toFutureOr
      outputs <- xsnService.getUnspentOutputs(address).toFutureOr
    } yield outputs

    result.toFuture
  }

  private def getAddress(addressString: String): ApplicationResult[Address] = {
    val maybe = Address.from(addressString)
    Or.from(maybe, One(AddressFormatError))
  }
}
