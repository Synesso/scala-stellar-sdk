package stellar.sdk.model.op

import stellar.sdk.PublicKeyOps
import stellar.sdk.model.xdr.Encode

/**
  * Requests that the network runs the inflation process.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#inflation endpoint doc]]
  */
case class InflationOperation(sourceAccount: Option[PublicKeyOps] = None) extends Operation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(9)
}
