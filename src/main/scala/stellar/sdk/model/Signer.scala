package stellar.sdk.model

import cats.data.State
import org.stellar.xdr
import org.stellar.xdr.{Uint32, Signer => XSigner}
import stellar.sdk.{KeyPair, PublicKey, PublicKeyOps}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays

case class Signer(
  key: SignerStrKey,
  weight: Int,
  sponsor: Option[PublicKey] = None
) extends Encodable {
  def xdr: XSigner = new XSigner.Builder()
    .key(key.signerXdr)
    .weight(new Uint32(weight))
    .build()

  def encode: LazyList[Byte] = key.encode ++ Encode.int(weight)
}

object Signer extends Decode {
  def decodeXdr(xdr: XSigner): Signer = Signer(
    key = SignerStrKey.decodeXdr(xdr.getKey),
    weight = xdr.getWeight.getUint32
  )

  def decode: State[Seq[Byte], Signer] = for {
    key <- StrKey.decode
    weight <- int
  } yield Signer(key, weight)
}
