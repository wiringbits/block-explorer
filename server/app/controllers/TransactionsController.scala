package controllers

import javax.inject.Inject

import com.xsn.explorer.services.TransactionService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class TransactionsController @Inject() (
    transactionService: TransactionService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getTransaction(txid: String) = publicNoInput { _ =>
    transactionService.getTransactionDetails(txid)
  }
}
