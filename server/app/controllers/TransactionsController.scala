package controllers

import com.xsn.explorer.models.request.SendRawTransactionRequest
import com.xsn.explorer.services.TransactionRPCService
import controllers.common.{MyJsonController, MyJsonControllerComponents}
import javax.inject.Inject

class TransactionsController @Inject() (
    transactionRPCService: TransactionRPCService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  import Context._

  def getTransaction(txid: String) = public { _ =>
    transactionRPCService.getTransactionDetails(txid)
  }

  def getRawTransaction(txid: String) = public { _ =>
    transactionRPCService.getRawTransaction(txid)
  }

  def sendRawTransaction() = publicInput { ctx: HasModel[SendRawTransactionRequest] =>
    transactionRPCService.sendRawTransaction(ctx.model.hex)
  }
}
