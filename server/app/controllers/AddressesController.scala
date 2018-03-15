package controllers

import javax.inject.Inject

import com.xsn.explorer.services.AddressService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class AddressesController @Inject() (
    addressService: AddressService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getDetails(address: String) = publicNoInput { _ =>
    addressService.getDetails(address)
  }
}
