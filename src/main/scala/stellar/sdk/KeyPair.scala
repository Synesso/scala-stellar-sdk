package stellar.sdk

import java.security.{MessageDigest, SignatureException}
import java.util.Arrays

import cats.data.State
import net.i2p.crypto.eddsa._
import net.i2p.crypto.eddsa.spec._
import stellar.sdk.model.StrKey
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays

import scala.concurrent.{ExecutionContext, Future}
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
  def sign(data: Array[Byte]): Signature = {
    val sig = new EdDSAEngine(MessageDigest.getInstance("SHA-512"))
    sig.initSign(sk)
    sig.update(data)

    Signature(sig.sign, hint)
  }

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

  override def toString: String = s"""PublicKey("$accountId")"""
}

sealed trait PublicKeyOps extends Encodable {
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

  /**
    * This key pair or verifying key without the private key.
    */
  def asPublicKey = PublicKey(pk)


  /**
    * A four-byte code that provides a hint to the identity of this public key
    */
  def hint: Array[Byte] = pk.getAbyte.drop(pk.getAbyte.length - 4)

  def encode: Stream[Byte] = Encode.int(0) ++ Encode.bytes(32, pk.getAbyte)
}

//noinspection ReferenceMustBePrefixed
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
    Arrays.fill(decoded, 0.toByte)
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
    Try {
      val decoded = StrKey.decodeStellarSecretSeed(charSeed)
      val kp = fromSecretSeed(decoded)
      Arrays.fill(charSeed, ' ')
      kp
    } match {
      case Failure(t) => throw InvalidSecretSeed(t)
      case Success(kp) => kp
    }
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
  def fromAccountId(accountId: String): PublicKey = Try(fromPublicKey(StrKey.decodeStellarAccountId(accountId))) match {
    case Success(pk) => pk
    case Failure(t) => throw InvalidAccountId(accountId, t)
  }

  /**
    * Returns the public key associated with the federated address.
    *
    * @param address formatted as `name*domain`, as per https://www.stellar.org/developers/guides/concepts/federation.html#stellar-addresses
    * @return Future[{ @link PublicKey }]
    */
  def fromAddress(address: String)(implicit ec: ExecutionContext): Future[PublicKey] = {
    address match {
      case AddressRegex(name, domain) =>
        DomainInfo.forDomain(s"https://$domain")
          .flatMap {
            case Some(info) => info.federationServer.byName(s"$name*$domain").map(_.map(_.account))
                .map(_.getOrElse(throw NoSuchAddress(address, new Exception(s"Address not found on ${info.federationServer}"))))
            case None => Future(throw NoSuchAddress(address, new Exception("Domain info could not be found")))
          }
          .recover {
            case t: NoSuchAddress => throw t
            case t: Throwable => throw NoSuchAddress(address, t)
          }
      case _ => Future(throw NoSuchAddress(address, new Exception("Not in correct form: name*domain")))
    }
  }

  private val AddressRegex = """(.+)\*(.+)""".r

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

case class InvalidAccountId(id: String, cause: Throwable) extends RuntimeException(id, cause)

case class InvalidSecretSeed(cause: Throwable) extends RuntimeException(cause)

case class NoSuchAddress(address: String, cause: Throwable) extends RuntimeException(address, cause)