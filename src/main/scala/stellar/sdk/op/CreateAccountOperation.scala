package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr._
import stellar.sdk.{KeyPair, NativeAmount, PublicKey, PublicKeyOps, _}

import scala.util.Try

/**
  * Funds and creates a new account.
  *
  * @param destinationAccount the account to be created
  * @param startingBalance the amount of funds to send to it
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#create-account endpoint doc]]
  */
case class CreateAccountOperation(destinationAccount: PublicKeyOps,
                                  startingBalance: NativeAmount = Amount.lumens(1),
                                  sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

  override def toOperationBody: OperationBody = {
    val op = new CreateAccountOp()
    val destination = new AccountID()
    destination.setAccountID(destinationAccount.getXDRPublicKey)
    op.setDestination(destination)
    val startBal = new Int64()
    startBal.setInt64(startingBalance.units)
    op.setStartingBalance(startBal)
    val body = new org.stellar.sdk.xdr.Operation.OperationBody()
    body.setDiscriminant(OperationType.CREATE_ACCOUNT)
    body.setCreateAccountOp(op)
    body
  }

  override def encode: Stream[Byte] = Encode.int(0) ++ destinationAccount.encode ++ Encode.long(startingBalance.units)
}

object CreateAccountOperation {

  def from(op: CreateAccountOp, source: Option[PublicKey]): Try[CreateAccountOperation] = Try {
    CreateAccountOperation(
      destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
      startingBalance = NativeAmount(op.getStartingBalance.getInt64.longValue),
      sourceAccount = source
    )
  }
}
