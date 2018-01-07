package stellar.scala.sdk

import java.security.{MessageDigest, SignatureException}
import java.util

import net.i2p.crypto.eddsa._
import net.i2p.crypto.eddsa.spec._

import scala.util.{Failure, Success, Try}

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
  def sign(data: Array[Byte]): Array[Byte] = Try {
    val sig = new EdDSAEngine(MessageDigest.getInstance("SHA-512"))
    sig.initSign(sk)
    sig.update(data)
    sig.sign
  } match {
    case Success(bs) => bs
    case Failure(t) => throw new RuntimeException(t)
  }

}

case class VerifyingKey(pk: EdDSAPublicKey) extends PublicKeyOps

trait PublicKeyOps {
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
  } match {
    case Success(b) => b
    case Failure(_: SignatureException) => false
    case Failure(t) => throw new RuntimeException(t)
  }

}

object KeyPair {

  private val ed25519 = EdDSANamedCurveTable.getByName("ed25519-sha-512")

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
    * Creates a new Stellar verifying key from a 32 byte address.
    *
    * @param publicKey The 32 byte public key.
    * @return { @link VerifyingKey}
    */
  def fromPublicKey(publicKey: Array[Byte]): VerifyingKey = {
    VerifyingKey(new EdDSAPublicKey(new EdDSAPublicKeySpec(publicKey, ed25519)))
  }

  /**
    * Generates a random Stellar keypair.
    *
    * @return a random Stellar keypair.
    */
  def random: KeyPair = {
    val pair = new KeyPairGenerator().generateKeyPair()
    KeyPair(pair.getPublic.asInstanceOf[EdDSAPublicKey], pair.getPrivate.asInstanceOf[EdDSAPrivateKey])
  }


}
