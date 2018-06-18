package controllers

import javax.inject.Inject

import com.alexitc.playsonify.models.{Limit, Offset, PaginatedQuery}
import com.xsn.explorer.services.{AddressService, TransactionService}
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class AddressesController @Inject() (
    addressService: AddressService,
    transactionService: TransactionService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getDetails(address: String) = publicNoInput { _ =>
    addressService.getDetails(address)
  }

  def getTransactions(address: String, offset: Int, limit: Int) = publicNoInput { _ =>
    val paginatedQuery = PaginatedQuery(Offset(offset), Limit(limit))

    transactionService.getTransactions(address, paginatedQuery)
  }

  def getUnspentOutputs(address: String) = publicNoInput { _ =>
    addressService.getUnspentOutputs(address)
  }
}
