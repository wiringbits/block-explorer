package com.xsn.explorer.models

import com.xsn.explorer.models.values.{Address, TransactionId}
import enumeratum._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.Try

case class TPoSContract(id: TPoSContract.Id, details: TPoSContract.Details, time: Long, state: TPoSContract.State) {

  val txid: TransactionId = id.txid
}

object TPoSContract {

  case class Id(txid: TransactionId, index: Int)
  class Commission private (val int: Int) extends AnyVal

  object Commission {

    val range = 0 to 100

    def from(int: Int): Option[Commission] = {
      if (range contains int) Some(new Commission(int))
      else None
    }

    implicit val reads: Reads[Commission] = Reads { json =>
      json.validate[Int].flatMap { value =>
        from(value)
          .map(JsSuccess.apply(_))
          .getOrElse {
            JsError.apply("Invalid commission")
          }
      }
    }
  }

  case class Details(owner: Address, merchant: Address, merchantCommission: Commission)

  object Details {

    /**
     * Try to get the contract details from the output script ASM.
     *
     * expected format:
     * - "OP_RETURN [hex_encoded_owner_address] [hex_encoded_merchant_address] [owner_commission] [signature]
     *
     * example:
     * - "OP_RETURN 5869337351664d51737932437a4d5a54726e4b573648464770315671465468644c77 58794a4338786e664672484e634d696e68366778755052595939484361593944416f 99"
     */
    def fromOutputScriptASM(asm: String): Option[Details] = {
      val parts = asm.split(" ").toList

      parts match {
        // signature after commission
        case op :: owner :: merchant :: commission :: _ :: Nil if op == "OP_RETURN" =>
          for {
            ownerAddress <- Address.fromHex(owner)
            merchantAddress <- Address.fromHex(merchant)
            ownerCommission <- Try(commission.toInt).toOption
            merchantCommission <- TPoSContract.Commission.from(100 - ownerCommission)
          } yield Details(owner = ownerAddress, merchant = merchantAddress, merchantCommission = merchantCommission)

        case _ => None
      }
    }

    implicit val detailsReads: Reads[Details] = (
      (JsPath \ "tposAddress").read[Address] and
        (JsPath \ "merchantAddress").read[Address] and
        (JsPath \ "commission").read[Commission]
    )(Details.apply _)

  }

  sealed abstract class State(override val entryName: String) extends EnumEntry

  object State extends Enum[State] {

    val values = findValues

    final case object Active extends State("ACTIVE")
    final case object Closed extends State("CLOSED")
  }

  implicit val writes: Writes[TPoSContract] = (obj: TPoSContract) => {
    Json.obj(
      "txid" -> obj.id.txid,
      "index" -> obj.id.index,
      "owner" -> obj.details.owner,
      "merchant" -> obj.details.merchant,
      "merchantCommission" -> obj.details.merchantCommission.int,
      "time" -> obj.time,
      "state" -> obj.state.entryName
    )
  }
}
