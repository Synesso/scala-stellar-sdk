package stellar.sdk.op

import org.stellar.sdk.xdr.CreatePassiveOfferOp
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.CREATE_PASSIVE_OFFER
import stellar.sdk.XDRPrimitives._
import stellar.sdk.{Amount, Asset, _}

import scala.util.Try

case class CreatePassiveOfferOperation(selling: Amount, buying: Asset, price: Price,
                                       sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def toOperationBody: OperationBody = {
    val op = new CreatePassiveOfferOp
    op.setSelling(selling.asset.toXDR)
    op.setBuying(buying.toXDR)
    op.setAmount(int64(selling.units))
    op.setPrice(price.toXDR)
    val body = new OperationBody
    body.setDiscriminant(CREATE_PASSIVE_OFFER)
    body.setCreatePassiveOfferOp(op)
    body
  }
}

object CreatePassiveOfferOperation {
  def from(op: CreatePassiveOfferOp, source: Option[PublicKey]): Try[CreatePassiveOfferOperation] = for {
    selling <- Asset.fromXDR(op.getSelling)
    buying <- Asset.fromXDR(op.getBuying)
    units = op.getAmount.getInt64.longValue
    price = Price(
      n = op.getPrice.getN.getInt32,
      d = op.getPrice.getD.getInt32
    )
  } yield {
    CreatePassiveOfferOperation(Amount(units, selling), buying, price, source)
  }
}
