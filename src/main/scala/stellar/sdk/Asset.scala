package stellar.sdk

import org.stellar.sdk.xdr.AssetType._
import org.stellar.sdk.xdr.{AccountID, AssetType, Asset => XDRAsset}
import stellar.sdk.ByteArrays._

import scala.util.{Success, Try}

sealed trait Asset {
  def toXDR: XDRAsset
}

object Asset {
  def apply(code: String, issuer: PublicKeyOps): NonNativeAsset =
    if (code.length <= 4) IssuedAsset4.of(code, issuer) else IssuedAsset12.of(code, issuer)

  def fromXDR(xdr: XDRAsset): Try[Asset] = Try {
    xdr.getDiscriminant match {
      case ASSET_TYPE_NATIVE => NativeAsset
      case ASSET_TYPE_CREDIT_ALPHANUM4 =>
        val code = paddedByteArrayToString(xdr.getAlphaNum4.getAssetCode)
        val issuer = KeyPair.fromXDRPublicKey(xdr.getAlphaNum4.getIssuer.getAccountID)
        IssuedAsset4.of(code, issuer)
      case ASSET_TYPE_CREDIT_ALPHANUM12 =>
        val code = paddedByteArrayToString(xdr.getAlphaNum12.getAssetCode)
        val issuer = KeyPair.fromXDRPublicKey(xdr.getAlphaNum12.getIssuer.getAccountID)
        IssuedAsset12.of(code, issuer)
    }
  }
}

case object NativeAsset extends Asset {
  override val toXDR: XDRAsset = {
    val xdr = new XDRAsset
    xdr.setDiscriminant(ASSET_TYPE_NATIVE)
    xdr
  }
}

trait NonNativeAsset extends Asset {
  val code: String
  val issuer: PublicKey
  val typeString: String
}

/**
  * Represents all assets with codes 1-4 characters long.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/assets.html" target="_blank">Assets</a>
  */
case class IssuedAsset4 private(code: String, issuer: PublicKey) extends NonNativeAsset {
  assert(code.nonEmpty, s"Asset's code '$code' cannot be empty")
  assert(code.length <= 4, s"Asset's code '$code' should have length no greater than 4")

  override def toXDR: XDRAsset = {
    val xdr = new XDRAsset
    xdr.setDiscriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM4)
    val credit = new XDRAsset.AssetAlphaNum4
    credit.setAssetCode(paddedByteArray(code, 4))
    val accountID = new AccountID
    accountID.setAccountID(issuer.getXDRPublicKey)
    credit.setIssuer(accountID)
    xdr.setAlphaNum4(credit)
    xdr
  }

  override val typeString = "credit_alphanum4"
}

object IssuedAsset4 {
  def of(code: String, issuer: PublicKeyOps): IssuedAsset4 = IssuedAsset4(code, issuer.asPublicKey)
}


/**
  * Represents all assets with codes 5-12 characters long.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/assets.html" target="_blank">Assets</a>
  */
case class IssuedAsset12 private (code: String, issuer: PublicKey) extends NonNativeAsset {
  assert(code.length >= 5 && code.length <= 12, s"Asset's code '$code' should have length between 5 & 12 inclusive")

  override def toXDR: XDRAsset = {
    val xdr = new XDRAsset
    xdr.setDiscriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM12)
    val credit = new XDRAsset.AssetAlphaNum12
    credit.setAssetCode(paddedByteArray(code, 12))
    val accountID = new AccountID
    accountID.setAccountID(issuer.getXDRPublicKey)
    credit.setIssuer(accountID)
    xdr.setAlphaNum12(credit)
    xdr
  }

  override val typeString = "credit_alphanum12"
}

object IssuedAsset12 {
  def of(code: String, keyPair: PublicKeyOps): IssuedAsset12 = IssuedAsset12(code, keyPair.asPublicKey)
}
