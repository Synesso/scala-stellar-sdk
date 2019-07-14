package stellar.sdk.model

import cats.data.State
import stellar.sdk.{KeyPair, PublicKeyOps}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays

case class Signer(key: SignerStrKey, weight: Int) extends Encodable {
  def encode: Stream[Byte] = key.encode ++ Encode.int(weight)
}

object Signer extends Decode {
  def decode: State[Seq[Byte], Signer] = for {
    key <- StrKey.decode
    weight <- int
  } yield Signer(key, weight)
}
