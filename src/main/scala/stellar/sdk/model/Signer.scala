package stellar.sdk.model

import org.stellar.xdr.{Uint32, Signer => XSigner}
import stellar.sdk.PublicKey

case class Signer(
  key: SignerStrKey,
  weight: Int,
  sponsor: Option[PublicKey] = None
) {
  def xdr: XSigner = new XSigner.Builder()
    .key(key.signerXdr)
    .weight(new Uint32(weight))
    .build()
}

object Signer {
  def decodeXdr(xdr: XSigner): Signer = Signer(
    key = SignerStrKey.decodeXdr(xdr.getKey),
    weight = xdr.getWeight.getUint32
  )
}
