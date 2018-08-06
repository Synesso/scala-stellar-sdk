package stellar.sdk.op

import org.stellar.sdk.xdr.AccountID
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.ACCOUNT_MERGE
import stellar.sdk.{KeyPair, PublicKey, PublicKeyOps}

import scala.util.Try

/**
  * Deletes account and transfers remaining balance to destination account.
  */
case class AccountMergeOperation(destination: PublicKeyOps, sourceAccount: Option[PublicKeyOps] = None) extends Operation {
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
  def from(body: OperationBody, source: Option[PublicKey]): Try[AccountMergeOperation] = Try {
    AccountMergeOperation(KeyPair.fromXDRPublicKey(body.getDestination.getAccountID), source)
  }
}
