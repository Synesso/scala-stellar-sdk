package stellar.sdk.model

import cats.data.State
import stellar.sdk.{KeyPair, PublicKey, PublicKeyOps}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays

case class Signer(
  key: SignerStrKey,
  weight: Int,
  sponsor: Option[PublicKey] = None
) extends Encodable {
  def encode: LazyList[Byte] = key.encode ++ Encode.int(weight)
}

object Signer extends Decode {
  def decode: State[Seq[Byte], Signer] = for {
    key <- StrKey.decode
    weight <- int
  } yield Signer(key, weight)
}
