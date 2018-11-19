package controllers

import javax.inject.Inject

import com.alexitc.playsonify.models.PublicContextWithModel
import com.xsn.explorer.models.Address
import com.xsn.explorer.models.request.SendRawTransactionRequest
import com.xsn.explorer.services.TransactionService
import controllers.common.{MyJsonController, MyJsonControllerComponents}

class TransactionsController @Inject() (
    transactionService: TransactionService,
    cc: MyJsonControllerComponents)
    extends MyJsonController(cc) {

  def getTransaction(txid: String) = publicNoInput { _ =>
    transactionService.getTransactionDetails(txid)
  }

  def getRawTransaction(txid: String) = publicNoInput { _ =>
    transactionService.getRawTransaction(txid)
  }

  def sendRawTransaction() = publicWithInput { ctx: PublicContextWithModel[SendRawTransactionRequest] =>
    transactionService.sendRawTransaction(ctx.model.hex)
  }

  def getLatestByAddresses() = publicWithInput { ctx: PublicContextWithModel[List[Address]] =>
    transactionService.getLatestTransactionBy(ctx.model)
  }
}
