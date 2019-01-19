package controllers

import com.xsn.explorer.models.request.SendRawTransactionRequest
import com.xsn.explorer.services.TransactionService
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject

class TransactionsController @Inject() (
    transactionService: TransactionService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Context._

  def getTransaction(txid: String) = public { _ =>
    transactionService.getTransactionDetails(txid)
  }

  def getRawTransaction(txid: String) = public { _ =>
    transactionService.getRawTransaction(txid)
  }

  def sendRawTransaction() = publicInput { ctx: HasModel[SendRawTransactionRequest] =>
    transactionService.sendRawTransaction(ctx.model.hex)
  }
}
