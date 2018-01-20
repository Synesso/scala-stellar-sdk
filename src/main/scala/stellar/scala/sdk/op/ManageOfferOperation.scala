package stellar.scala.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{ManageOfferOp, Price => XDRPrice}
import stellar.scala.sdk._

import scala.util.{Failure, Success, Try}

sealed trait ManageOfferOperation extends Operation {
  val offerId: Long = 0
}

case class CreateOfferOperation(selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[KeyPair] = None) extends ManageOfferOperation {

  override def toOperationBody: OperationBody = {
    val op = new ManageOfferOp
    op.setSelling(selling.asset.toXDR)
    op.setBuying(buying.toXDR)
    op.setAmount(int64(selling.units))
    op.setPrice(price.toXDR)
    val body = new OperationBody
    body.setDiscriminant(MANAGE_OFFER)
    body.setManageOfferOp(op)
    body
  }

}

case class DeleteOfferOperation(override val offerId: Long,
                                sourceAccount: Option[KeyPair] = None) extends ManageOfferOperation {

  override def toOperationBody: OperationBody = {
    val op = new ManageOfferOp
    op.setOfferID(uint64(offerId))
    op.setAmount(int64(0))
    val body = new OperationBody
    body.setDiscriminant(MANAGE_OFFER)
    body.setManageOfferOp(op)
    body
  }
}

case class UpdateOfferOperation(override val offerId: Long,
                                selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[KeyPair] = None) extends ManageOfferOperation {
  override def toOperationBody: OperationBody = {
    val op = new ManageOfferOp
    op.setOfferID(uint64(offerId))
    op.setSelling(selling.asset.toXDR)
    op.setBuying(buying.toXDR)
    op.setAmount(int64(selling.units))
    op.setPrice(price.toXDR)
    val body = new OperationBody
    body.setDiscriminant(MANAGE_OFFER)
    body.setManageOfferOp(op)
    body
  }
}

object ManageOfferOperation {
  def from(op: ManageOfferOp): Try[ManageOfferOperation] = {
    val offerId = Option(op.getOfferID).map(_.getUint64.longValue)

    val offerDetails = for {
      selling <- Option(op.getSelling)
      buying <- Option(op.getBuying)
      amount <- Option(op.getAmount).map(_.getInt64.longValue)
      price <- Option(op.getPrice).map(p => Price(
        n = p.getN.getInt32.intValue,
        d = p.getD.getInt32.intValue
      ))
    } yield {
      (selling, buying, amount, price)
    }

    (offerId, offerDetails) match {
      case (Some(id), None) => Success(DeleteOfferOperation(id))

      case (None, Some((selling, buying, amount, price))) => for {
        sell <- Asset.fromXDR(selling)
        buy <- Asset.fromXDR(buying)
      } yield CreateOfferOperation(Amount(amount, sell), buy, price)

      case (Some(id), Some((selling, buying, amount, price))) => for {
        sell <- Asset.fromXDR(selling)
        buy <- Asset.fromXDR(buying)
      } yield UpdateOfferOperation(id, Amount(amount, sell), buy, price)

      case _ => Failure(new IllegalArgumentException(s"ManageOfferOp did not define a valid operation: $op"))
    }
  }
}

case class Price(n: Int, d: Int) extends XDRPrimitives {
  def toXDR = {
    val xdr = new XDRPrice
    xdr.setN(int32(n))
    xdr.setD(int32(d))
    xdr
  }

}
