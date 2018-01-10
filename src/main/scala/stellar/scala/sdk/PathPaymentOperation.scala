package stellar.scala.sdk

import org.stellar.sdk.xdr.Operation.OperationBody

case class PathPaymentOperation(sendMax: Amount,
                                destinationAccount: PublicKeyOps,
                                destinationAmount: Amount,
                                path: Seq[Asset],
                                sourceAccount: Option[KeyPair] = None) extends Operation {

  override def toOperationBody: OperationBody = ???

}
