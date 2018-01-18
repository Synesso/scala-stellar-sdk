package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{AccountID, Int32, Int64, String32, Uint32, Operation => XDROp}

import scala.util.{Failure, Try}

trait Operation extends XDRPrimitives {
  val sourceAccount: Option[KeyPair]

  def toOperationBody: OperationBody

  def toXDR: XDROp = {
    val op = new org.stellar.sdk.xdr.Operation()
    sourceAccount.foreach { sa =>
      val src = new AccountID()
      src.setAccountID(sa.getXDRPublicKey)
      op.setSourceAccount(src)
    }
    op.setBody(toOperationBody)
    op
  }
}

object Operation {

  val ONE = BigDecimal(10).pow(7)

  def fromXDR(op: XDROp): Try[Operation] = {
    op.getBody.getDiscriminant match {
      case ALLOW_TRUST => AllowTrustOperation.from(op.getBody.getAllowTrustOp)
      case CHANGE_TRUST => ChangeTrustOperation.from(op.getBody.getChangeTrustOp)
      case CREATE_ACCOUNT => CreateAccountOperation.from(op.getBody.getCreateAccountOp)
      case PATH_PAYMENT => PathPaymentOperation.from(op.getBody.getPathPaymentOp)
      case PAYMENT => PaymentOperation.from(op.getBody.getPaymentOp)
      case SET_OPTIONS => SetOptionsOperation.from(op.getBody.getSetOptionsOp)
      case MANAGE_OFFER => ManageOfferOperation.from(op.getBody.getManageOfferOp)
      case CREATE_PASSIVE_OFFER => CreatePassiveOfferOperation.from(op.getBody.getCreatePassiveOfferOp)
      case d => Failure(new IllegalArgumentException(s"Unrecognised operation discriminant: $d"))
    }
  }
}
