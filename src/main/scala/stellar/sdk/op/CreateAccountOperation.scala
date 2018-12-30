package stellar.sdk.op

import cats.data.State
import stellar.sdk.xdr.{Decode, Encode}
import stellar.sdk.{KeyPair, NativeAmount, PublicKeyOps, _}

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

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(0) ++
      destinationAccount.encode ++
      Encode.long(startingBalance.units)
}

object CreateAccountOperation {
  val decode: State[Seq[Byte], CreateAccountOperation] = for {
    destination <- KeyPair.decode
    startingBalance <- Decode.long
  } yield CreateAccountOperation(destination, NativeAmount(startingBalance))
}
