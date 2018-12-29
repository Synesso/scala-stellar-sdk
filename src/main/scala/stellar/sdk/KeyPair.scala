package stellar.sdk

import java.security.{MessageDigest, SignatureException}
import java.util

import cats.data.State
import net.i2p.crypto.eddsa._
import net.i2p.crypto.eddsa.spec._
import stellar.sdk.xdr.{Decode, Encodable, Encode}

import scala.util.Try

case class KeyPair(pk: EdDSAPublicKey, sk: EdDSAPrivateKey) extends PublicKeyOps {

  /**
    * Returns the human readable secret seed encoded in strkey.
    */
  def secretSeed: Array[Char] = StrKey.encodeStellarSecretSeed(sk.getSeed)

  /**
    * Sign the provided data with the private key.
    *
    * @param data The data to sign.
    * @return signed bytes.
    */
  def sign(data: Array[Byte]): Signature = {
    val sig = new EdDSAEngine(MessageDigest.getInstance("SHA-512"))
    sig.initSign(sk)
    sig.update(data)

    Signature(sig.sign, hint)
  }

  /*
    * Sign the provided data with the private key.
    *
    * @param data The data to sign.
    * @return signed data in XDR format
    */
/*
  def signToXDR(data: Array[Byte]): DecoratedSignature = {
    val hint = signatureHint
    val signedData = sign(data).data
    val signature = new org.stellar.sdk.xdr.Signature
    signature.setSignature(signedData)
    val xdr = new DecoratedSignature
    xdr.setHint(hint)
    xdr.setSignature(signature)
    xdr
  }
*/

  override def toString: String = {
    s"""KeyPair("$accountId", "S...")"""
  }
}

case class PublicKey(pk: EdDSAPublicKey) extends PublicKeyOps {

  override def hashCode(): Int = accountId.hashCode()

  override def equals(obj: scala.Any): Boolean = obj match {
    case pubKey: PublicKeyOps => pubKey.accountId == accountId
    case _ => false
  }

  override def toString: String = s"PublicKey($accountId)"

}

trait PublicKeyOps extends Encodable {
  val pk: EdDSAPublicKey

  /**
    * @return the human readable account ID
    */
  def accountId: String = StrKey.encodeStellarAccountId(pk.getAbyte)

  def publicKey: Array[Byte] = pk.getAbyte

  /**
    * Verify the provided data and signature match.
    *
    * @param data      The data that was signed.
    * @param signature The signature.
    * @return True if they match, false otherwise.
    */
  def verify(data: Array[Byte], signature: Array[Byte]): Boolean = Try {
    val sig = new EdDSAEngine(MessageDigest.getInstance("SHA-512"))
    sig.initVerify(pk)
    sig.update(data)
    sig.verify(signature)
  }.recover {
    case _: SignatureException => false
  }.get

/*
  def getXDRPublicKey: XDRPublicKey = {
    val publicKey = new XDRPublicKey
    publicKey.setDiscriminant(PublicKeyType.PUBLIC_KEY_TYPE_ED25519)
    val uint256 = new Uint256
    uint256.setUint256(pk.getAbyte)
    publicKey.setEd25519(uint256)
    publicKey
  }
*/

/*
  def getXDRSignerKey: SignerKey = {
    val signerKey = new SignerKey
    signerKey.setDiscriminant(SignerKeyType.SIGNER_KEY_TYPE_ED25519)
    val uint256 = new Uint256
    uint256.setUint256(pk.getAbyte)
    signerKey.setEd25519(uint256)
    signerKey
  }
*/

  /**
    * This key pair or verifying key without the private key.
    */
  def asPublicKey = PublicKey(pk)


  /**
    * A four-byte code that provides a hint to the identity of this public key
    */
  def hint: Array[Byte] = pk.getAbyte.drop(pk.getAbyte.length - 4)

  /**
    * XDR entity derived from this public key for use in signatures
    */
/*
  val signatureHint: SignatureHint = {
    val pkStream = new ByteArrayOutputStream
    val os = new XdrDataOutputStream(pkStream)
    XDRPublicKey.encode(os, getXDRPublicKey)
    val pkBytes = pkStream.toByteArray
    val hintBytes = util.Arrays.copyOfRange(pkBytes, pkBytes.length - 4, pkBytes.length)
    val hint = new SignatureHint
    hint.setSignatureHint(hintBytes)
    hint
  }
*/

  def encode: Stream[Byte] = Encode.int(0) ++ Encode.bytes(32, pk.getAbyte)

  override def toString: String = s"""PublicKey("$accountId")"""

}

object KeyPair {

  private val ed25519 = EdDSANamedCurveTable.getByName("ed25519")

  /**
    * Creates a new Stellar KeyPair from a strkey encoded Stellar secret seed.
    *
    * @param seed Char array containing strkey encoded Stellar secret seed.
    * @return { @link KeyPair}
    */
  def fromSecretSeed(seed: Array[Char]): KeyPair = {
    val decoded = StrKey.decodeStellarSecretSeed(seed)
    val kp = fromSecretSeed(decoded)
    util.Arrays.fill(decoded, 0.toByte)
    kp
  }

  /**
    * <strong>Insecure</strong> Creates a new Stellar KeyPair from a strkey encoded Stellar secret seed.
    * This method is <u>insecure</u>. Use only if you are aware of security implications.
    *
    * @see <a href="http://docs.oracle.com/javase/1.5.0/docs/guide/security/jce/JCERefGuide.html#PBEEx" target="_blank">Using Password-Based Encryption</a>
    * @param seed The strkey encoded Stellar secret seed.
    * @return { @link KeyPair}
    */
  def fromSecretSeed(seed: String): KeyPair = {
    val charSeed = seed.toCharArray
    val decoded = StrKey.decodeStellarSecretSeed(charSeed)
    val kp = fromSecretSeed(decoded)
    util.Arrays.fill(charSeed, ' ')
    kp
  }

  /**
    * Creates a new Stellar keypair from a raw 32 byte secret seed.
    *
    * @param seed The 32 byte secret seed.
    * @return { @link KeyPair}
    */
  def fromSecretSeed(seed: Array[Byte]): KeyPair = {
    val privKeySpec = new EdDSAPrivateKeySpec(seed, ed25519)
    val publicKeySpec = new EdDSAPublicKeySpec(privKeySpec.getA.toByteArray, ed25519)
    KeyPair(new EdDSAPublicKey(publicKeySpec), new EdDSAPrivateKey(privKeySpec))
  }

  /**
    * Creates a new Stellar keypair from a passphrase
    *
    * @param passphrase the secret passphrase.
    * @return ( @link KeyPair }
    */
  def fromPassphrase(passphrase: String): KeyPair = fromSecretSeed(ByteArrays.sha256(passphrase.getBytes("UTF-8")))

  /**
    * Creates a new Stellar verifying key from a 32 byte address.
    *
    * @param publicKey The 32 byte public key.
    * @return { @link PublicKey }
    */
  def fromPublicKey(publicKey: Array[Byte]): PublicKey = {
    PublicKey(new EdDSAPublicKey(new EdDSAPublicKeySpec(publicKey, ed25519)))
  }

  /**
    * Creates a new Stellar PublicKey from a strkey encoded Stellar account ID.
    *
    * @param accountId The strkey encoded Stellar account ID.
    * @return { @link PublicKey}
    */
  def fromAccountId(accountId: String): PublicKey = fromPublicKey(StrKey.decodeStellarAccountId(accountId))

//  def fromXDRPublicKey(key: XDRPublicKey) = fromPublicKey(key.getEd25519.getUint256)

  /**
    * Generates a random Stellar keypair.
    *
    * @return a random Stellar keypair.
    */
  def random: KeyPair = {
    val pair = new KeyPairGenerator().generateKeyPair()
    KeyPair(pair.getPublic.asInstanceOf[EdDSAPublicKey], pair.getPrivate.asInstanceOf[EdDSAPrivateKey])
  }

  def decode: State[Seq[Byte], PublicKey] = for {
    _ <- Decode.int
    bs <- Decode.bytes(32)
  } yield KeyPair.fromPublicKey(bs.toArray)

  def decodeXDR(base64: String): PublicKey = decode.run(ByteArrays.base64(base64)).value._2

}

case class Signature(data: Array[Byte], hint: Array[Byte]) extends Encodable {
  def encode: Stream[Byte] = Encode.bytes(4, hint) ++ Encode.bytes(data)
}

object Signature {
  def decode: State[Seq[Byte], Signature] = for {
    hint <- Decode.bytes(4).map(_.toArray)
    data <- Decode.bytes.map(_.toArray)
  } yield Signature(data, hint)
}