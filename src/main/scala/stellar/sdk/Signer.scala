package stellar.sdk

sealed trait Signer extends Encodable {
  val weight: Int
}

case class AccountSigner(key: PublicKeyOps, weight: Int) extends Signer {
  override def encode: Stream[Byte] = Encode.int(0) ++ Encode.bytes(key.publicKey) ++ Encode.int(weight)
}

case class PreAuthTxnSigner(hash: String, weight: Int) extends Signer {
  override def encode: Stream[Byte] = Encode.int(1) ++ Encode.bytes(ByteArrays.base64(hash)) ++ Encode.int(weight)
}

case class HashSigner(hash: String, weight: Int) extends Signer {
  override def encode: Stream[Byte] = Encode.int(2) ++ Encode.bytes(ByteArrays.base64(hash)) ++ Encode.int(weight)
}
