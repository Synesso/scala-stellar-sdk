package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{AccountID, Operation => XDROp}

import scala.util.{Failure, Try}

trait Operation {
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
      case CHANGE_TRUST => ChangeTrustOperation.from(op.getBody.getChangeTrustOp)
      case CREATE_ACCOUNT => CreateAccountOperation.from(op.getBody.getCreateAccountOp)
      case PATH_PAYMENT => PathPaymentOperation.from(op.getBody.getPathPaymentOp)
      case PAYMENT => PaymentOperation.from(op.getBody.getPaymentOp)
      case d => Failure(new IllegalArgumentException(s"Unrecognised operation discriminant: $d"))
    }
  }
}
