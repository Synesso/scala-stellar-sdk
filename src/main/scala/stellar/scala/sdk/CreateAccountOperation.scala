package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr._

/**
  * Represents <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html#create-account" target="_blank">CreateAccount</a> operation.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html" target="_blank">List of Operations</a>
  */
case class CreateAccountOperation(destinationAccount: PublicKeyOps,
                                  startingBalance: Amount = Amount(0),
                                  sourceAccount: Option[KeyPair] = None) extends Operation {

  override def toOperationBody: OperationBody = {
    val op = new CreateAccountOp()
    val destination = new AccountID()
    destination.setAccountID(destinationAccount.getXDRPublicKey)
    op.setDestination(destination)
    val startBal = new Int64()
    startBal.setInt64(startingBalance.stroops)
    op.setStartingBalance(startBal)
    val body = new org.stellar.sdk.xdr.Operation.OperationBody()
    body.setDiscriminant(OperationType.CREATE_ACCOUNT)
    body.setCreateAccountOp(op)
    body
  }

}

object CreateAccountOperation {

  def apply(sourceAccount: KeyPair,
            destinationAccount: PublicKeyOps,
            startingBalance: Amount): CreateAccountOperation = {
    CreateAccountOperation(destinationAccount, startingBalance, Some(sourceAccount))
  }

  def from(op: CreateAccountOp): CreateAccountOperation = CreateAccountOperation(
    sourceAccount = None,
    destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
    startingBalance = Amount(op.getStartingBalance.getInt64.longValue)
  )
}
