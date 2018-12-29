package stellar.sdk

import cats.data.State
import stellar.sdk.xdr.{Decode, Encodable, Encode}

sealed trait Signer extends Encodable {
  val weight: Int
}

object Signer {
  def decode: State[Seq[Byte], Signer] = for {
    discriminant <- Decode.int
    bs <- Decode.bytes(32).map(_.toArray)
    weight <- Decode.int
  } yield discriminant match {
    case 0 => AccountSigner(KeyPair.fromPublicKey(bs), weight)
    case 1 => PreAuthTxnSigner(ByteArrays.base64(bs), weight)
    case 2 => HashSigner(ByteArrays.base64(bs), weight)
  }
}

case class AccountSigner(key: PublicKeyOps, weight: Int) extends Signer {
  def encode: Stream[Byte] = Encode.int(0) ++ Encode.bytes(32, key.publicKey) ++ Encode.int(weight)
}

case class PreAuthTxnSigner(hash: String, weight: Int) extends Signer {
  def encode: Stream[Byte] = Encode.int(1) ++ Encode.bytes(32, ByteArrays.base64(hash)) ++ Encode.int(weight)
}

case class HashSigner(hash: String, weight: Int) extends Signer {
  def encode: Stream[Byte] = Encode.int(2) ++ Encode.bytes(32, ByteArrays.base64(hash)) ++ Encode.int(weight)
}
