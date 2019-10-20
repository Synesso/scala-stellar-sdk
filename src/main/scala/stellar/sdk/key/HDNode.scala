package stellar.sdk.key

import okio.{Buffer, ByteString}
import stellar.sdk.KeyPair

class HDNode(val privateKey: ByteString, val chainCode: ByteString) {
  def asKeyPair: KeyPair = KeyPair.fromSecretSeed(privateKey)

  def deriveChild(index: Int, ix: Int*): HDNode = {
    ix.foldLeft(deriveChild(index)) { case (node, index) => node.deriveChild(index) }
  }

  private def deriveChild(index: Int): HDNode = {
    val key = new Buffer()
      .write(Array(0.toByte))
      .write(privateKey)
      .writeInt((HDNode.hardenedMinIndex + index).toInt)
      .readByteString()
     HDNode.fromHmac(key.hmacSha512(chainCode))
  }
}

object HDNode {

  protected val hardenedMinIndex = 0x80000000L

  def fromEntropy(entropy: ByteString): HDNode =
    HDNode.fromHmac(entropy.hmacSha512(ByteString.encodeUtf8("ed25519 seed")))

  def fromHmac(hmac: ByteString): HDNode = {
    val bytes = hmac.toByteArray
    new HDNode(new ByteString(bytes.take(32)), new ByteString(bytes.drop(32)))
  }
}
