package stellar.scala.sdk
import org.stellar.sdk.xdr.AccountID
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.ACCOUNT_MERGE

import scala.util.Try

case class AccountMergeOperation(destination: PublicKeyOps, sourceAccount: Option[KeyPair] = None) extends Operation {
  override def toOperationBody: OperationBody = {
    val body = new OperationBody
    val id = new AccountID
    id.setAccountID(destination.getXDRPublicKey)
    body.setDestination(id)
    body.setDiscriminant(ACCOUNT_MERGE)
    body
  }
}

object AccountMergeOperation {
  def from(body: OperationBody): Try[AccountMergeOperation] = Try {
    AccountMergeOperation(KeyPair.fromXDRPublicKey(body.getDestination.getAccountID))
  }
}
